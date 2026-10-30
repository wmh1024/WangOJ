/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge;

import cn.wmhwiki.judge.pojo.dto.JudgeTestCase;
import cn.wmhwiki.judge.pojo.vo.JudgeCaseResult;
import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 沙箱协议模型的序列化单元测试（不启动 Spring、不访问 Docker）。
 */
class ProtocolJsonTest {

    private final Gson gson = new Gson();

    @Test
    void caseResultKeepsAllFields() {
        JudgeCaseResult result = new JudgeCaseResult(2, "ACCEPTED", "42\n", "", 15L, 4096L);
        String json = gson.toJson(result);
        JudgeCaseResult parsed = gson.fromJson(json, JudgeCaseResult.class);

        assertEquals(2, parsed.getIndex());
        assertEquals("ACCEPTED", parsed.getVerdict());
        assertEquals("42\n", parsed.getStdout());
        assertEquals(15L, parsed.getTimeMs());
        assertEquals(4096L, parsed.getMemoryBytes());
    }

    @Test
    void compileErrorResponseHasNoCases() {
        JudgeResponse response = JudgeResponse.success("req-9");
        response.setVerdict(JudgeResponse.VERDICT_COMPILE_ERROR);
        response.setMessage("compilation error");
        response.setCompileOutput("error: expected ';'");
        response.setCases(Arrays.asList());
        response.setTotalTimeMs(0L);
        response.setMaxMemoryBytes(0L);

        String json = gson.toJson(response);
        assertTrue(json.contains("\"verdict\":\"COMPILE_ERROR\""));
        assertTrue(json.contains("\"cases\":[]"));
    }

    @Test
    void errorResponseUsesHttpLikeCode() {
        JudgeResponse response = JudgeResponse.error("req-1", 401, "鉴权失败");
        assertEquals("error", response.getStatus());
        assertEquals(401, response.getCode());
        assertEquals("SYSTEM_ERROR", response.getVerdict());
    }

    @Test
    void testCaseAllowsNullOrEmpty() {
        JudgeTestCase testCase = new JudgeTestCase(null, null);
        String json = gson.toJson(testCase);
        // Gson 默认不序列化 null 字段，因此只断言不抛异常且为合法 JSON
        assertTrue(json.startsWith("{"));
    }
}
