/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.service;

import cn.wmhwiki.judge.constant.Constants;
import cn.wmhwiki.judge.constant.enums.LangContextEnum;
import cn.wmhwiki.judge.exception.JudgeException;
import cn.wmhwiki.judge.pojo.dto.JudgeLimits;
import cn.wmhwiki.judge.pojo.dto.JudgeRequest;
import cn.wmhwiki.judge.pojo.dto.JudgeTestCase;
import cn.wmhwiki.judge.pojo.lang.LangContext;
import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import cn.wmhwiki.judge.utils.DockerUtil;
import cn.wmhwiki.judge.utils.FileUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.UUID;

/**
 * 判题服务：校验协议请求，落盘源码与测试输入，交给 {@link DockerUtil} 执行。
 *
 * <p>沙箱只负责执行，不判定答案（不读取 {@code expectedOutput}），
 * 答案比较由后端完成。</p>
 */
@Service
public class JudgeService {

    private static final Logger log = LoggerFactory.getLogger(JudgeService.class);

    public JudgeResponse judge(JudgeRequest request) {
        String requestId = normalizeRequestId(request.getRequestId());
        validateProtocolVersion(requestId, request.getProtocolVersion());
        validateMode(requestId, request.getMode());

        String code = request.getCode();
        if (code == null || code.trim().isEmpty()) {
            throw new JudgeException(400, requestId, "代码不能为空");
        }
        if (code.length() > Constants.maxCodeLength) {
            throw new JudgeException(413, requestId, "代码长度不能超过 " + Constants.maxCodeLength + " 个字符");
        }

        String language = request.getLanguage();
        if (language == null || language.trim().isEmpty()) {
            throw new JudgeException(400, requestId, "语言不能为空");
        }
        LangContextEnum langContextEnum = LangContextEnum.getByLang(language.trim().toLowerCase());
        if (langContextEnum == null) {
            throw new JudgeException(400, requestId, "暂不支持 " + language + " 语言");
        }
        LangContext langContext = langContextEnum.getLangContext();

        JudgeLimits limits = normalizeLimits(requestId, request.getLimits());
        List<JudgeTestCase> testCases = normalizeTestCases(requestId, request.getTestCases());

        String folderPath = Constants.codeSourcePath + File.separator + UUID.randomUUID();
        try {
            // 存放源码
            FileUtil.saveFile(folderPath, langContext.getSourceFileName(), code);
            // 存放测试输入
            for (int i = 0; i < testCases.size(); i++) {
                String input = testCases.get(i).getInput();
                FileUtil.saveFile(folderPath, i + ".in", input == null ? "" : input);
            }
            log.info("开始判题, requestId={}, language={}, cases={}, timeLimitMs={}, memoryLimitMb={}",
                    requestId, langContext.getLang(), testCases.size(),
                    limits.getTimeLimitMs(), limits.getMemoryLimitMb());
            return DockerUtil.execute(requestId, langContext, limits, testCases.size(), folderPath);
        } finally {
            // 无论成功失败都清理运行目录（FileUtil.delete 对不存在的路径是幂等的）
            FileUtil.delete(new File(folderPath));
        }
    }

    private String normalizeRequestId(String requestId) {
        if (requestId == null || requestId.trim().isEmpty()) {
            return UUID.randomUUID().toString();
        }
        String trimmed = requestId.trim();
        // 只允许安全字符，避免脏数据进入日志
        if (trimmed.length() > 128 || !trimmed.matches("[A-Za-z0-9_.:-]+")) {
            return UUID.randomUUID().toString();
        }
        return trimmed;
    }

    private void validateProtocolVersion(String requestId, String protocolVersion) {
        if (protocolVersion == null || protocolVersion.trim().isEmpty()) {
            // 兼容早期客户端：缺省视为 1.0
            return;
        }
        if (!Constants.protocolVersion.equals(protocolVersion.trim())) {
            throw new JudgeException(400, requestId,
                    "不支持的协议版本：" + protocolVersion + "，当前支持 " + Constants.protocolVersion);
        }
    }

    private void validateMode(String requestId, String mode) {
        if (mode == null || mode.trim().isEmpty()) {
            return;
        }
        if (!Constants.MODE_JUDGE.equals(mode) && !Constants.MODE_RUN.equals(mode)) {
            throw new JudgeException(400, requestId, "不支持的模式：" + mode);
        }
    }

    /**
     * 归一化资源限制：null 或越界值回落/收敛到安全区间，避免调用方传 0 或超大值。
     */
    private JudgeLimits normalizeLimits(String requestId, JudgeLimits limits) {
        JudgeLimits result = new JudgeLimits();
        long timeLimitMs = limits == null || limits.getTimeLimitMs() == null
                ? Constants.defaultTimeLimitMs : limits.getTimeLimitMs();
        if (timeLimitMs <= 0) {
            throw new JudgeException(400, requestId, "timeLimitMs 必须为正数");
        }
        result.setTimeLimitMs(clamp(timeLimitMs, Constants.minTimeLimitMs, Constants.maxTimeLimitMs));

        long memoryLimitMb = limits == null || limits.getMemoryLimitMb() == null
                ? Constants.defaultMemoryLimitMb : limits.getMemoryLimitMb();
        if (memoryLimitMb <= 0) {
            throw new JudgeException(400, requestId, "memoryLimitMb 必须为正数");
        }
        result.setMemoryLimitMb(clamp(memoryLimitMb, Constants.minMemoryLimitMb, Constants.maxMemoryLimitMb));

        long compileTimeLimitMs = limits == null || limits.getCompileTimeLimitMs() == null
                ? Constants.compileTimeLimit : limits.getCompileTimeLimitMs();
        result.setCompileTimeLimitMs(clamp(compileTimeLimitMs,
                Constants.minCompileTimeLimitMs, Constants.maxCompileTimeLimitMs));

        long maxOutputBytes = limits == null || limits.getMaxOutputBytes() == null
                ? Constants.maxStdoutLength : limits.getMaxOutputBytes();
        result.setMaxOutputBytes(clamp(maxOutputBytes, 1L, Constants.maxStdoutLength));
        return result;
    }

    private List<JudgeTestCase> normalizeTestCases(String requestId, List<JudgeTestCase> testCases) {
        if (testCases == null || testCases.isEmpty()) {
            throw new JudgeException(400, requestId, "至少需要一个测试点");
        }
        if (testCases.size() > Constants.maxTestCases) {
            throw new JudgeException(413, requestId,
                    "测试点数量不能超过 " + Constants.maxTestCases + " 个");
        }
        for (JudgeTestCase testCase : testCases) {
            if (testCase == null) {
                throw new JudgeException(400, requestId, "测试点不能为空");
            }
            if (testCase.getInput() != null && testCase.getInput().length() > Constants.maxInputLength) {
                throw new JudgeException(413, requestId,
                        "单个测试输入不能超过 " + Constants.maxInputLength + " 个字符");
            }
            // expectedOutput 已废弃：后端不再下发。旧客户端若仍携带，只做长度校验后丢弃，
            // 既不参与判定，也不写入磁盘。
            String legacyExpected = testCase.getExpectedOutput();
            if (legacyExpected != null && legacyExpected.length() > Constants.maxInputLength) {
                throw new JudgeException(413, requestId,
                        "expectedOutput 已废弃且不能超过 " + Constants.maxInputLength + " 个字符");
            }
        }
        return testCases;
    }

    private static long clamp(long value, long min, long max) {
        return Math.max(min, Math.min(value, max));
    }
}
