package cn.wmhwiki.wangojbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.wmhwiki.wangojbackend.dto.CommitlogDTO;
import cn.wmhwiki.wangojbackend.pojo.LimitOption;
import cn.wmhwiki.wangojbackend.WangojBackendApplication.JudgeQueueFullException;
import cn.wmhwiki.wangojbackend.pojo.RankItem;
import cn.wmhwiki.wangojbackend.pojo.TestCase;
import cn.wmhwiki.wangojbackend.vo.CommitlogVO;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.constant.JudgeLimits;
import cn.wmhwiki.wangojbackend.constant.JudgeStatus;
import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import cn.wmhwiki.wangojbackend.constant.RedisTimeoutConstant;
import cn.wmhwiki.wangojbackend.constant.SupportedLanguage;
import cn.wmhwiki.wangojbackend.domain.Commitlog;
import cn.wmhwiki.wangojbackend.domain.Problem;
import cn.wmhwiki.wangojbackend.exception.BusinessException;
import cn.wmhwiki.wangojbackend.filter.RequestIdFilter;
import cn.wmhwiki.wangojbackend.mapper.CommitlogMapper;
import cn.wmhwiki.wangojbackend.mapper.ProblemMapper;
import cn.wmhwiki.wangojbackend.service.CommitlogService;
import cn.wmhwiki.wangojbackend.service.JudgeClient;
import cn.wmhwiki.wangojbackend.service.ProblemService;
import cn.wmhwiki.wangojbackend.util.JudgeOutputUtils;
import cn.wmhwiki.wangojbackend.util.PageUtils;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.core.task.TaskRejectedException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

/**
 * @author WangOJ contributors
 * @description 针对表【commitLog】的数据库操作Service实现
 * @createDate 2024-03-19 18:46:59
 */
@Service
@Slf4j
public class CommitlogServiceImpl extends ServiceImpl<CommitlogMapper, Commitlog> implements CommitlogService {

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    /**
     * 代码最大长度（字符）
     */
    private static final int MAX_CODE_LENGTH = 64 * 1024;

    /**
     * 编译错误信息最多落库的字符数，避免把超长编译器输出写进数据库
     */
    private static final int MAX_ERROR_MESSAGE_CHARS = 8 * 1024;

    /**
     * 判题结果落库重试次数
     */
    private static final int STATUS_UPDATE_RETRY = 3;

    @Resource
    private CommitlogMapper commitlogMapper;

    @Resource
    private ProblemService problemService;

    @Resource
    private ProblemMapper problemMapper;

    @Resource
    private JudgeClient judgeClient;

    @Resource
    private Gson gson;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisTimeoutConstant timeout;

    @Resource
    private ThreadPoolTaskExecutor taskExecutor;

    @Override
    public String commitCode(CommitlogDTO commitlogDTO) {
        if (commitlogDTO == null || commitlogDTO.getProblemId() == null) {
            throw new BusinessException("题目不能为空");
        }
        Long problemId = commitlogDTO.getProblemId();
        String code = commitlogDTO.getCode();
        String lang = SupportedLanguage.normalize(commitlogDTO.getLang());
        if (StrUtil.isBlank(code)) {
            throw new BusinessException("代码不能为空");
        }
        if (code.length() > MAX_CODE_LENGTH) {
            throw new BusinessException("代码长度不能超过 " + MAX_CODE_LENGTH / 1024 + "KB");
        }
        if (StrUtil.isBlank(lang)) {
            throw new BusinessException("请选择编程语言");
        }
        if (!SupportedLanguage.isSupported(lang)) {
            throw new BusinessException("暂不支持该编程语言");
        }

        Problem problem = problemMapper.selectById(problemId);
        if (problem == null) {
            throw BusinessException.notFound("题目不存在");
        }
        List<TestCase> testCases = parseTestCases(problem.getTestCase());
        if (testCases.isEmpty()) {
            throw new BusinessException("该题目尚未配置测试用例");
        }
        if (testCases.size() > JudgeLimits.MAX_TEST_CASES) {
            throw new BusinessException("测试用例数量不能超过 " + JudgeLimits.MAX_TEST_CASES + " 个");
        }

        LimitOption limitOption = parseLimitOption(problem.getLimitOption());
        long timeLimitMs = JudgeLimits.normalizeTimeLimit(limitOption.getTimeoutLimit());
        long memoryLimitMb = JudgeLimits.normalizeMemoryLimit(limitOption.getMemoryLimit());

        UserVO userVO = UserHolder.getUserVO();
        if (userVO == null) {
            throw BusinessException.unauthorized("请先登录");
        }
        Commitlog commitlog = new Commitlog();
        commitlog.setUserId(userVO.getId());
        commitlog.setProblemId(problemId);
        commitlog.setLang(lang);
        commitlog.setCode(code);
        commitlog.setStatus(JudgeStatus.WAITING);
        if (commitlogMapper.insert(commitlog) == 0) {
            throw new BusinessException("提交失败");
        }

        // 判题请求 ID：commit-<id>-<uuid>，贯穿后端日志与沙箱日志
        String requestId = "commit-" + commitlog.getId() + "-" + UUID.randomUUID();
        // 把当前 HTTP 请求的 requestId 带进判题线程，方便从一次点击追到判题结果
        String traceId = MDC.get(RequestIdFilter.MDC_KEY);

        JudgeTask task = new JudgeTask(commitlog.getId(), requestId, problemId, code, lang,
                timeLimitMs, memoryLimitMb, testCases);
        try {
            taskExecutor.execute(() -> {
                if (traceId != null) {
                    MDC.put(RequestIdFilter.MDC_KEY, traceId);
                }
                try {
                    task.run();
                } catch (Exception e) {
                    log.error("判题任务执行失败, requestId={}, commitlogId={}", requestId, commitlog.getId(), e);
                    markSystemError(commitlog.getId());
                } finally {
                    // 线程复用，必须清理，避免串号
                    MDC.remove(RequestIdFilter.MDC_KEY);
                }
            });
        } catch (JudgeQueueFullException | TaskRejectedException e) {
            // 队列已满 / 线程池关闭：提交已落库，必须给出明确状态，避免永久 Waiting
            log.error("判题任务提交失败（队列已满）, requestId={}, commitlogId={}", requestId, commitlog.getId());
            markSystemError(commitlog.getId());
            throw new BusinessException(503, "判题队列繁忙，请稍后重试");
        }

        return "ok";
    }

    /**
     * 一次判题任务的不可变描述。
     */
    private final class JudgeTask {

        private final Long commitlogId;
        private final String requestId;
        private final Long problemId;
        private final String code;
        private final String language;
        private final long timeLimitMs;
        private final long memoryLimitMb;
        private final List<TestCase> testCases;

        private JudgeTask(Long commitlogId, String requestId, Long problemId, String code, String language,
                          long timeLimitMs, long memoryLimitMb, List<TestCase> testCases) {
            this.commitlogId = commitlogId;
            this.requestId = requestId;
            this.problemId = problemId;
            this.code = code;
            this.language = language;
            this.timeLimitMs = timeLimitMs;
            this.memoryLimitMb = memoryLimitMb;
            this.testCases = testCases;
        }

        private void run() {
            executeJudge(this);
        }
    }

    /**
     * 调用沙箱判题，并逐测试点比较结果。
     *
     * <p>注意：这是同类内部调用，不能再加 {@code @Async}（代理不生效），
     * 异步由外层 {@link ThreadPoolTaskExecutor} 负责。</p>
     */
    private void executeJudge(JudgeTask task) {
        String requestId = task.requestId;
        Long commitlogId = task.commitlogId;
        try {
            JudgeClient.JudgeResult result = judgeClient.judge(requestId, task.language, task.code,
                    task.testCases, task.timeLimitMs, task.memoryLimitMb);
            if (result.isCompileError()) {
                log.info("编译错误, requestId={}, commitlogId={}", requestId, commitlogId);
                updateCommitlog(commitlogId, JudgeStatus.COMPILE_ERROR, new LimitOption(0L, 0L),
                        truncate(result.getCompileOutput(), MAX_ERROR_MESSAGE_CHARS), null);
                return;
            }
            if (!result.isExecuted()) {
                log.error("沙箱未能完成执行, requestId={}, commitlogId={}, message={}",
                        requestId, commitlogId, result.getMessage());
                markSystemError(commitlogId);
                return;
            }

            JudgeOutcome outcome = compareTestCases(task, result);
            log.info("判题完成, requestId={}, commitlogId={}, status={}, timeMs={}, memoryBytes={}",
                    requestId, commitlogId, outcome.status, outcome.totalTimeMs, outcome.maxMemoryBytes);
            updateCommitlog(commitlogId, outcome.status,
                    new LimitOption(outcome.totalTimeMs, outcome.maxMemoryBytes),
                    outcome.detailJson(), null);

            updateProblemAfterJudge(task, outcome);
        } catch (Exception e) {
            log.error("判题过程发生错误, requestId={}, commitlogId={}", requestId, commitlogId, e);
            markSystemError(commitlogId);
        }
    }

    /**
     * 更新题目的提交/通过计数与相关缓存。
     *
     * <p>这是判题主结果的附属动作：即使数据库瞬时失败，也不能把已经落库的 AC/WA
     * 覆盖成 System Error。计数失败只记日志，下一轮判题或人工修复即可追平。</p>
     */
    private void updateProblemAfterJudge(JudgeTask task, JudgeOutcome outcome) {
        try {
            if (JudgeStatus.ACCEPT.equals(outcome.status)) {
                problemMapper.plusAcceptNum(task.problemId);
                // 有人通过会影响排行榜，主动失效缓存而不是等 5 分钟过期
                evictRankCache();
            } else {
                problemMapper.plusCommitNum(task.problemId);
            }
        } catch (Exception e) {
            log.error("题目计数更新失败，已保留判题结果, requestId={}, commitlogId={}, problemId={}",
                    task.requestId, task.commitlogId, task.problemId, e);
        }
        evictProblemContent(task.problemId);
    }

    /**
     * 逐测试点比较输出并汇总最终状态。
     */
    private JudgeOutcome compareTestCases(JudgeTask task, JudgeClient.JudgeResult judgeResult) {
        List<TestCase> testCases = task.testCases;
        List<JudgeClient.CaseResult> cases = judgeResult.getCases();
        List<CaseOutcome> outcomes = new ArrayList<>(testCases.size());
        List<String> caseVerdicts = new ArrayList<>();
        long totalTime = 0L;
        long maxMemory = 0L;

        for (int i = 0; i < testCases.size(); i++) {
            TestCase testCase = testCases.get(i);
            if (cases == null || i >= cases.size() || cases.get(i) == null) {
                // 沙箱没有返回该测试点 => 系统错误
                caseVerdicts.add(JudgeStatus.CASE_SYSTEM_ERROR);
                outcomes.add(new CaseOutcome(i, JudgeStatus.CASE_SYSTEM_ERROR, 0L, 0L, false));
                continue;
            }
            JudgeClient.CaseResult judgeCase = cases.get(i);
            Long timeMs = judgeCase.getTimeMs();
            Long memoryBytes = judgeCase.getMemoryBytes();
            long timeForTotal = timeMs == null ? 0L : timeMs;
            long memoryForTotal = memoryBytes == null ? 0L : memoryBytes;
            totalTime += timeForTotal;
            maxMemory = Math.max(maxMemory, memoryForTotal);

            String verdict = JudgeOutputUtils.resolveCaseVerdict(
                    judgeCase.getVerdict(),
                    testCase.getOutput(),
                    judgeCase.getStdout(),
                    timeMs,
                    memoryBytes == null ? null : memoryBytes / 1024,
                    task.timeLimitMs,
                    task.memoryLimitMb * 1024);
            caseVerdicts.add(verdict);
            outcomes.add(new CaseOutcome(i, verdict, timeMs, memoryBytes,
                    JudgeStatus.CASE_ACCEPTED.equals(verdict)));
        }

        String status = JudgeOutputUtils.aggregateVerdict(caseVerdicts);
        return new JudgeOutcome(status, totalTime, maxMemory, gson.toJson(outcomes));
    }

    @Override
    public PageVO<List<CommitlogVO>> getCommitVOList(Integer current, Integer pageSize, Long problemId, Long userId) {
        int pageCurrent = PageUtils.page(current);
        int pageSizeValue = PageUtils.pageSize(pageSize);

        QueryWrapper<Commitlog> qw = new QueryWrapper<>();
        if (problemId != null) {
            qw.eq("problemId", problemId);
        }
        if (userId != null) {
            qw.eq("userId", userId);
        }
        qw.orderByDesc("id");

        Page<Commitlog> page = new Page<>(pageCurrent, pageSizeValue);
        page = commitlogMapper.selectPage(page, qw);
        List<Commitlog> commitLogs = page.getRecords();

        // 批量查询题目，避免 N+1
        Set<Long> problemIds = commitLogs.stream()
                .map(Commitlog::getProblemId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));
        Map<Long, Problem> problemMap = new HashMap<>();
        if (!problemIds.isEmpty()) {
            problemService.listByIds(problemIds)
                    .forEach(problem -> problemMap.put(problem.getId(), problem));
        }

        List<CommitlogVO> commitlogVOs = new ArrayList<>(commitLogs.size());
        for (Commitlog commitLog : commitLogs) {
            CommitlogVO commitlogVO = new CommitlogVO();
            commitlogVO.setId(commitLog.getId());
            commitlogVO.setUserId(commitLog.getUserId());
            commitlogVO.setProblemId(commitLog.getProblemId());
            Problem problem = problemMap.get(commitLog.getProblemId());
            commitlogVO.setProblemTitle(problem == null ? "题目已删除" : problem.getTitle());
            commitlogVO.setStatus(commitLog.getStatus());
            commitlogVO.setLang(commitLog.getLang());
            LimitOption limitOption = parseLimitOptionSafe(commitLog.getLimitOption());
            if (limitOption != null) {
                commitlogVO.setTimeoutLimit(limitOption.getTimeoutLimit());
                commitlogVO.setMemoryLimit(limitOption.getMemoryLimit());
            }
            commitlogVO.setCreatedTime(formatTime(commitLog.getCreatedTime()));
            commitlogVOs.add(commitlogVO);
        }
        PageVO<List<CommitlogVO>> listPageVO = new PageVO<>();
        listPageVO.setT(commitlogVOs);
        listPageVO.setTotal(page.getTotal());
        listPageVO.setCurrent(page.getCurrent());
        listPageVO.setSize(page.getSize());
        return listPageVO;
    }

    /**
     * 解析统计信息。
     *
     * <p><b>权限说明：</b>本方法被 {@code GET /api/log/{id}} 调用而不需要登录，
     * 即提交记录（含源代码）默认对匿名用户可读——这是本项目的产品策略（OJ 惯例），
     * 已写入 README「安全说明」与 SECURITY.md 的信任边界表。</p>
     *
     * <p>不在此接口下发题面：题面统一走 {@code /api/problem/content}，避免同一份数据两个来源。
     * 返回的 {@code caseResult} 只含 verdict/耗时/内存，不含期望输出。</p>
     */
    @Override
    public CommitlogVO getCommitlogById(Long id) {
        if (id == null) {
            throw new BusinessException("提交记录ID不能为空");
        }
        Commitlog commitlog = commitlogMapper.selectById(id);
        if (commitlog == null) {
            throw BusinessException.notFound("提交记录不存在");
        }
        Problem problem = commitlog.getProblemId() == null
                ? null : problemMapper.selectById(commitlog.getProblemId());
        CommitlogVO commitlogVO = new CommitlogVO();
        commitlogVO.setId(commitlog.getId());
        commitlogVO.setUserId(commitlog.getUserId());
        commitlogVO.setProblemId(commitlog.getProblemId());
        commitlogVO.setStatus(commitlog.getStatus());
        commitlogVO.setLang(commitlog.getLang());
        commitlogVO.setCode(commitlog.getCode());
        commitlogVO.setErrorMessage(commitlog.getErrorMessage());
        commitlogVO.setCaseResult(commitlog.getCaseResult());
        if (problem != null) {
            commitlogVO.setProblemTitle(problem.getTitle());
            commitlogVO.setProblemTags(parseStringList(problem.getTags()));
        } else {
            commitlogVO.setProblemTitle("题目已删除");
            commitlogVO.setProblemTags(new ArrayList<>());
        }

        LimitOption limitOption = parseLimitOptionSafe(commitlog.getLimitOption());
        if (limitOption != null) {
            commitlogVO.setTimeoutLimit(limitOption.getTimeoutLimit());
            commitlogVO.setMemoryLimit(limitOption.getMemoryLimit());
        }
        commitlogVO.setCreatedTime(formatTime(commitlog.getCreatedTime()));
        return commitlogVO;
    }

    @Override
    public List<RankItem> getRankList() {
        try {
            String rankJson = stringRedisTemplate.opsForValue().get(RedisKeyConstant.RANK_LIST);
            if (StrUtil.isNotBlank(rankJson)) {
                List<RankItem> cached = gson.fromJson(rankJson,
                        new TypeToken<List<RankItem>>() {
                        }.getType());
                if (cached != null) {
                    return cached;
                }
            }
        } catch (Exception e) {
            // 缓存损坏或 Redis 不可用时回源数据库
            log.warn("读取排行榜缓存失败: {}", e.getMessage());
        }
        List<RankItem> rankList = commitlogMapper.getRankList();
        try {
            stringRedisTemplate.opsForValue().set(RedisKeyConstant.RANK_LIST, gson.toJson(rankList),
                    timeout.getRankList(), TimeUnit.MINUTES);
        } catch (Exception e) {
            log.warn("写入排行榜缓存失败: {}", e.getMessage());
        }
        return rankList;
    }

    /**
     * 排行榜缓存主动失效（有人 AC 时调用）。
     */
    private void evictRankCache() {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.RANK_LIST);
        } catch (Exception e) {
            log.warn("清理排行榜缓存失败: {}", e.getMessage());
        }
    }

    /**
     * 题目通过数/提交数变化后，题目详情缓存也需失效。
     */
    private void evictProblemContent(Long problemId) {
        if (problemId == null) {
            return;
        }
        try {
            stringRedisTemplate.delete(RedisKeyConstant.PROBLEM_CONTENT + problemId);
        } catch (Exception e) {
            log.warn("清理题目缓存失败, problemId={}: {}", problemId, e.getMessage());
        }
    }

    /**
     * 把判题结论写回数据库并重试几次。
     *
     * @param commitlogId  提交 ID
     * @param status       最终状态
     * @param limitOption  耗时/内存
     * @param errorMessage 编译错误信息（可为 null）
     */
    private void markSystemError(Long commitlogId) {
        updateCommitlog(commitlogId, JudgeStatus.SYSTEM_ERROR, new LimitOption(0L, 0L),
                "判题服务异常，请稍后重试或联系管理员", null);
    }

    private void updateCommitlog(Long commitlogId, String status, LimitOption limitOption,
                                 String errorMessage, String caseResult) {
        Exception lastError = null;
        for (int attempt = 1; attempt <= STATUS_UPDATE_RETRY; attempt++) {
            try {
                Commitlog update = new Commitlog();
                update.setStatus(status);
                update.setLimitOption(gson.toJson(limitOption));
                // 用 UpdateWrapper.set 显式写入 null（MyBatis-Plus 默认忽略实体中的 null 字段）
                UpdateWrapper<Commitlog> wrapper = new UpdateWrapper<Commitlog>().eq("id", commitlogId);
                wrapper.set("status", status);
                wrapper.set("limitOption", gson.toJson(limitOption));
                if (errorMessage != null) {
                    wrapper.set("errorMessage", errorMessage);
                } else {
                    wrapper.set("errorMessage", null);
                }
                if (caseResult != null) {
                    wrapper.set("caseResult", caseResult);
                }
                int affected = commitlogMapper.update(update, wrapper);
                if (affected <= 0) {
                    throw new IllegalStateException("更新提交记录失败：affected=" + affected);
                }
                return;
            } catch (Exception e) {
                lastError = e;
                log.warn("判题结果落库失败（第 {} 次）, commitlogId={}: {}", attempt, commitlogId, e.getMessage());
                try {
                    TimeUnit.MILLISECONDS.sleep(200L * attempt);
                } catch (InterruptedException ie) {
                    Thread.currentThread().interrupt();
                    break;
                }
            }
        }
        log.error("判题结果落库最终失败, commitlogId={}, status={}", commitlogId, status, lastError);
    }

    private List<TestCase> parseTestCases(String testCaseJson) {
        if (StrUtil.isBlank(testCaseJson)) {
            return new ArrayList<>();
        }
        try {
            List<TestCase> testCases = gson.fromJson(testCaseJson,
                    new TypeToken<List<TestCase>>() {
                    }.getType());
            if (testCases == null) {
                return new ArrayList<>();
            }
            return testCases.stream()
                    .filter(Objects::nonNull)
                    .peek(testCase -> {
                        if (testCase.getInput() == null) {
                            testCase.setInput("");
                        }
                        if (testCase.getOutput() == null) {
                            testCase.setOutput("");
                        }
                    })
                    .collect(Collectors.toList());
        } catch (JsonSyntaxException e) {
            throw new BusinessException("测试用例格式错误");
        }
    }

    private LimitOption parseLimitOption(String limitOptionJson) {
        LimitOption limitOption = parseLimitOptionSafe(limitOptionJson);
        return limitOption == null
                ? new LimitOption(JudgeLimits.DEFAULT_TIME_LIMIT_MS, JudgeLimits.DEFAULT_MEMORY_LIMIT_MB)
                : limitOption;
    }

    private LimitOption parseLimitOptionSafe(String limitOptionJson) {
        if (StrUtil.isBlank(limitOptionJson)) {
            return null;
        }
        try {
            return gson.fromJson(limitOptionJson, LimitOption.class);
        } catch (JsonSyntaxException e) {
            return null;
        }
    }

    private List<String> parseStringList(String json) {
        if (StrUtil.isBlank(json)) {
            return new ArrayList<>();
        }
        try {
            List<String> list = gson.fromJson(json, new TypeToken<List<String>>() {
            }.getType());
            return list == null ? new ArrayList<>() : list;
        } catch (JsonSyntaxException e) {
            return new ArrayList<>();
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return null;
        }
        return text.length() <= maxLength ? text : text.substring(0, maxLength) + "\n...(已截断)";
    }

    private static String formatTime(Date date) {
        if (date == null) {
            return null;
        }
        return TIME_FORMATTER.format(LocalDateTime.ofInstant(date.toInstant(), ZoneId.systemDefault()));
    }

    /**
     * 单个测试点的结论（落库用）。
     */
    private static final class CaseOutcome {
        private final int index;
        private final String verdict;
        private final Long timeMs;
        private final Long memoryBytes;
        private final boolean accepted;

        private CaseOutcome(int index, String verdict, Long timeMs, Long memoryBytes, boolean accepted) {
            this.index = index;
            this.verdict = verdict;
            this.timeMs = timeMs;
            this.memoryBytes = memoryBytes;
            this.accepted = accepted;
        }
    }

    private static final class JudgeOutcome {
        private final String status;
        private final long totalTimeMs;
        private final long maxMemoryBytes;
        private final String detailJson;

        private JudgeOutcome(String status, long totalTimeMs, long maxMemoryBytes, String detailJson) {
            this.status = status;
            this.totalTimeMs = totalTimeMs;
            this.maxMemoryBytes = maxMemoryBytes;
            this.detailJson = detailJson;
        }

        private String detailJson() {
            return detailJson;
        }
    }
}
