package cn.wmhwiki.wangojbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.wmhwiki.wangojbackend.dto.ProblemDTO;
import cn.wmhwiki.wangojbackend.dto.query.ProblemQuery;
import cn.wmhwiki.wangojbackend.pojo.LimitOption;
import cn.wmhwiki.wangojbackend.pojo.TestCase;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.ProblemVO;
import cn.wmhwiki.wangojbackend.constant.JudgeLimits;
import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import cn.wmhwiki.wangojbackend.constant.RedisTimeoutConstant;
import cn.wmhwiki.wangojbackend.domain.Problem;
import cn.wmhwiki.wangojbackend.exception.BusinessException;
import cn.wmhwiki.wangojbackend.mapper.ProblemMapper;
import cn.wmhwiki.wangojbackend.service.ProblemService;
import cn.wmhwiki.wangojbackend.util.PageUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * @author WangOJ contributors
 * @description 针对表【problem】的数据库操作Service实现
 * @createDate 2024-03-17 17:54:01
 */
@Service
@Slf4j
public class ProblemServiceImpl extends ServiceImpl<ProblemMapper, Problem> implements ProblemService {

    /**
     * 题库列表/热门列表最多返回的条数
     */
    private static final int HOT_PROBLEM_LIMIT = 10;

    /**
     * problem.tags 列宽为 VARCHAR(512)，序列化后的 JSON 不能超过此长度。
     */
    private static final int MAX_TAGS_JSON_LENGTH = 512;

    @Resource
    private ProblemMapper problemMapper;

    @Resource
    private Gson gson;

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private RedisTimeoutConstant timeout;

    @Override
    public PageVO<List<ProblemVO>> getProblemDTOList(Integer current, Integer pageSize, ProblemQuery problemQuery) {
        int pageCurrent = PageUtils.page(current);
        int pageSizeValue = PageUtils.pageSize(pageSize);
        QueryWrapper<Problem> qw = new QueryWrapper<>();
        if (problemQuery != null) {
            if (StrUtil.isNotBlank(problemQuery.getTitle())) {
                qw.like("title", problemQuery.getTitle());
            }
            if (problemQuery.getId() != null) {
                qw.eq("id", problemQuery.getId());
            }
            List<String> tags = problemQuery.getTags();
            if (tags != null) {
                for (String tag : tags) {
                    if (StrUtil.isNotBlank(tag)) {
                        qw.like("tags", tag);
                    }
                }
            }
        }

        // 分页查询（按 id 倒序，保证结果稳定）
        qw.orderByDesc("id");
        Page<Problem> page = new Page<>(pageCurrent, pageSizeValue);
        page = problemMapper.selectPage(page, qw);

        List<ProblemVO> problemVOList = new ArrayList<>(page.getRecords().size());
        for (Problem problem : page.getRecords()) {
            problemVOList.add(toSummaryVO(problem));
        }
        PageVO<List<ProblemVO>> pageVO = new PageVO<>();
        pageVO.setT(problemVOList);
        pageVO.setTotal(page.getTotal());
        pageVO.setCurrent(page.getCurrent());
        pageVO.setSize(page.getSize());
        return pageVO;
    }

    @Override
    public ProblemVO getProblemContent(Long id) {
        if (id == null) {
            throw new BusinessException("题目ID不能为空");
        }
        String cacheKey = RedisKeyConstant.PROBLEM_CONTENT + id;
        String problemJson = null;
        try {
            problemJson = stringRedisTemplate.opsForValue().get(cacheKey);
        } catch (Exception e) {
            // Redis 不可用时回退到数据库，不影响读题
            log.warn("读取题目缓存失败: {}", e.getMessage());
        }
        if (StrUtil.isNotBlank(problemJson)) {
            ProblemVO cached = parseCachedProblem(id, problemJson);
            if (cached != null) {
                return cached;
            }
            // 缓存损坏：删除后回源
            deleteCacheQuietly(cacheKey);
        }
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw BusinessException.notFound("题目不存在");
        }
        ProblemVO problemVO = toDetailVO(problem);
        try {
            stringRedisTemplate.opsForValue().set(cacheKey, gson.toJson(problemVO),
                    timeout.getProblemContent(), TimeUnit.HOURS);
        } catch (Exception e) {
            log.warn("写入题目缓存失败: {}", e.getMessage());
        }
        return problemVO;
    }

    /**
     * 解析缓存中的题目；损坏时返回 null，由调用方回源。
     */
    private ProblemVO parseCachedProblem(Long id, String problemJson) {
        try {
            ProblemVO problemVO = gson.fromJson(problemJson, ProblemVO.class);
            if (problemVO != null && problemVO.getId() != null) {
                return problemVO;
            }
        } catch (Exception e) {
            // 忽略，交由调用方回源
        }
        log.warn("题目缓存数据损坏，将回源数据库, id={}", id);
        return null;
    }

    @Override
    public String addProblem(ProblemDTO problemDTO) {
        validateProblem(problemDTO, false);
        Problem problem = new Problem();
        problem.setTitle(problemDTO.getTitle());
        problem.setContent(problemDTO.getContent());
        problem.setTags(gson.toJson(problemDTO.getTags() == null ? new ArrayList<String>() : problemDTO.getTags()));
        problem.setTestCase(gson.toJson(problemDTO.getTestCase()));
        problem.setLimitOption(gson.toJson(normalizeLimitOption(problemDTO.getLimitOption())));
        if (!save(problem)) {
            throw new BusinessException("保存失败!");
        }
        return "ok";
    }

    @Override
    public Long delProblem(Long id) {
        if (id == null) {
            throw new BusinessException("题目ID不能为空");
        }
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw BusinessException.notFound("题目不存在");
        }
        problemMapper.deleteById(id);
        clearProblemCache(id);
        return id;
    }

    @Override
    public Long changeProblem(ProblemDTO problemDTO) {
        validateProblem(problemDTO, true);
        Long id = problemDTO.getId();
        Problem problem = new Problem();
        problem.setId(id);
        problem.setTitle(problemDTO.getTitle());
        problem.setContent(problemDTO.getContent());
        problem.setTags(gson.toJson(problemDTO.getTags() == null ? new ArrayList<String>() : problemDTO.getTags()));
        problem.setTestCase(gson.toJson(problemDTO.getTestCase()));
        problem.setLimitOption(gson.toJson(normalizeLimitOption(problemDTO.getLimitOption())));
        // updateById 只更新非 null 字段，不会把 commitNum/acceptNum/isDeleted 覆盖成 null
        if (problemMapper.updateById(problem) == 0) {
            throw BusinessException.notFound("题目不存在或修改失败");
        }
        clearProblemCache(id);
        return id;
    }

    @Override
    public ProblemDTO getProblemByAdmin(Long id) {
        if (id == null) {
            throw new BusinessException("题目ID不能为空");
        }
        Problem problem = problemMapper.selectById(id);
        if (problem == null) {
            throw BusinessException.notFound("题目不存在");
        }
        ProblemDTO problemDTO = new ProblemDTO();
        problemDTO.setId(id);
        problemDTO.setTitle(problem.getTitle());
        problemDTO.setContent(problem.getContent());
        problemDTO.setTags(parseTags(problem.getTags()));
        problemDTO.setTestCase(parseTestCases(problem.getTestCase()));
        problemDTO.setLimitOption(parseLimitOption(problem.getLimitOption()));
        return problemDTO;
    }

    @Override
    public List<ProblemVO> getHotProblemList() {
        QueryWrapper<Problem> qw = new QueryWrapper<>();
        // 参数化 LIMIT，避免拼接 SQL 片段
        qw.orderBy(true, false, "acceptNum", "commitNum")
                .last("LIMIT " + HOT_PROBLEM_LIMIT);
        List<Problem> list = problemMapper.selectList(qw);
        List<ProblemVO> problemVOList = new ArrayList<>(list.size());
        for (Problem problem : list) {
            problemVOList.add(toSummaryVO(problem));
        }
        return problemVOList;
    }

    /**
     * 清理题目相关缓存。
     */
    private void clearProblemCache(Long id) {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.PROBLEM_CONTENT + id);
            // 排行榜依赖题目数据，一并失效
            stringRedisTemplate.delete(RedisKeyConstant.RANK_LIST);
        } catch (Exception e) {
            log.warn("清理题目缓存失败, id={}: {}", id, e.getMessage());
        }
    }

    private void deleteCacheQuietly(String cacheKey) {
        try {
            stringRedisTemplate.delete(cacheKey);
        } catch (Exception ignored) {
            // 删除失败不影响本次回源
        }
    }

    /**
     * 新增/修改题目的公共校验。
     */
    private void validateProblem(ProblemDTO problemDTO, boolean requireId) {
        if (problemDTO == null) {
            throw new BusinessException("题目内容不能为空");
        }
        if (requireId && problemDTO.getId() == null) {
            throw new BusinessException("题目ID不能为空");
        }
        if (StrUtil.isBlank(problemDTO.getTitle())) {
            throw new BusinessException("题目标题不能为空");
        }
        if (problemDTO.getTestCase() == null || problemDTO.getTestCase().isEmpty()) {
            throw new BusinessException("至少需要一个测试用例");
        }
        for (TestCase testCase : problemDTO.getTestCase()) {
            if (testCase == null) {
                throw new BusinessException("测试用例不能为空");
            }
        }
        String tagsJson = gson.toJson(problemDTO.getTags() == null
                ? new ArrayList<String>() : problemDTO.getTags());
        if (tagsJson.length() > MAX_TAGS_JSON_LENGTH) {
            throw new BusinessException("题目标签总长度不能超过 " + MAX_TAGS_JSON_LENGTH + " 个字符");
        }
    }

    /**
     * 时间/内存限制的默认值与边界收敛，避免把 0 或超大值写进题目。
     */
    private LimitOption normalizeLimitOption(LimitOption limitOption) {
        Long timeLimit = limitOption == null ? null : limitOption.getTimeoutLimit();
        Long memoryLimit = limitOption == null ? null : limitOption.getMemoryLimit();
        return new LimitOption(
                JudgeLimits.normalizeTimeLimit(timeLimit),
                JudgeLimits.normalizeMemoryLimit(memoryLimit));
    }

    /**
     * 列表展示用 VO（不含题面与测试用例）。
     */
    private ProblemVO toSummaryVO(Problem problem) {
        ProblemVO vo = new ProblemVO();
        vo.setId(problem.getId());
        vo.setTitle(problem.getTitle());
        vo.setTags(parseTags(problem.getTags()));
        vo.setCommitNum(problem.getCommitNum());
        vo.setAcceptNum(problem.getAcceptNum());
        return vo;
    }

    /**
     * 详情展示用 VO（含题面与限制，不含测试用例）。
     */
    private ProblemVO toDetailVO(Problem problem) {
        ProblemVO vo = toSummaryVO(problem);
        vo.setContent(problem.getContent());
        vo.setLimitOption(parseLimitOption(problem.getLimitOption()));
        return vo;
    }

    /**
     * 解析测试用例 JSON，坏数据返回空列表而不是抛异常。
     */
    private List<TestCase> parseTestCases(String testCaseJson) {
        if (StrUtil.isBlank(testCaseJson)) {
            return new ArrayList<>();
        }
        try {
            List<TestCase> list = gson.fromJson(testCaseJson, new TypeToken<List<TestCase>>() {
            }.getType());
            return list == null ? new ArrayList<>() : list;
        } catch (Exception e) {
            log.warn("测试用例 JSON 解析失败，已返回空列表: {}", e.getMessage());
            return new ArrayList<>();
        }
    }

    /**
     * 解析时间/内存限制 JSON，坏数据返回默认值。
     */
    private LimitOption parseLimitOption(String limitOptionJson) {
        if (StrUtil.isBlank(limitOptionJson)) {
            return normalizeLimitOption(null);
        }
        try {
            return normalizeLimitOption(gson.fromJson(limitOptionJson, LimitOption.class));
        } catch (Exception e) {
            log.warn("限制 JSON 解析失败，已返回默认值: {}", e.getMessage());
            return normalizeLimitOption(null);
        }
    }

    /**
     * 解析标签 JSON，坏数据返回空列表。
     */
    private List<String> parseTags(String tagsJson) {
        if (StrUtil.isBlank(tagsJson)) {
            return new ArrayList<>();
        }
        try {
            List<String> tags = gson.fromJson(tagsJson, new TypeToken<List<String>>() {
            }.getType());
            return tags == null ? new ArrayList<>() : tags;
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }
}
