/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge;

import cn.wmhwiki.judge.pojo.dto.JudgeLimits;
import cn.wmhwiki.judge.pojo.dto.JudgeRequest;
import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import com.google.gson.Gson;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 纯单元测试：不启动 Spring、不访问 Docker。
 */
class JudgeApplicationTests {

    private final Gson gson = new Gson();

    @Test
    void protocolJsonRoundTrip() {
        JudgeRequest request = new JudgeRequest();
        request.setRequestId("commit-1-abc");
        request.setProtocolVersion("1.0");
        request.setLanguage("cpp");
        request.setCode("int main() { return 0; }");
        request.setMode("judge");
        request.setLimits(new JudgeLimits(1000L, 256L, 5000L, 4096L));
        request.setTestCases(Collections.singletonList(
                new cn.wmhwiki.judge.pojo.dto.JudgeTestCase("1 2\n", "3\n")));

        String json = gson.toJson(request);
        JudgeRequest parsed = gson.fromJson(json, JudgeRequest.class);

        assertEquals("commit-1-abc", parsed.getRequestId());
        assertEquals("cpp", parsed.getLanguage());
        assertEquals(1, parsed.getTestCases().size());
        assertEquals("1 2\n", parsed.getTestCases().get(0).getInput());
        assertEquals(1000L, parsed.getLimits().getTimeLimitMs());
    }

    @Test
    void errorResponseUsesProtocolShape() {
        JudgeResponse response = JudgeResponse.error("req-1", 401, "鉴权失败");
        assertEquals("error", response.getStatus());
        assertEquals(401, response.getCode());
        assertEquals("SYSTEM_ERROR", response.getVerdict());
        String json = gson.toJson(response);
        assertTrue(json.contains("\"requestId\":\"req-1\""));
        assertTrue(json.contains("\"message\":\"鉴权失败\""));
    }

    @Test
    void caseResultSerializes() {
        cn.wmhwiki.judge.pojo.vo.JudgeCaseResult caseResult =
                new cn.wmhwiki.judge.pojo.vo.JudgeCaseResult(0, "ACCEPTED", "3\n", "", 12L, 2048L);
        String json = gson.toJson(Arrays.asList(caseResult));
        assertNotNull(json);
        assertTrue(json.contains("\"index\":0"));
        assertTrue(json.contains("\"timeMs\":12"));
    }
}
