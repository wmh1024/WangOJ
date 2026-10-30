package cn.wmhwiki.wangojbackend.service;

import cn.wmhwiki.wangojbackend.pojo.TestCase;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.sun.net.httpserver.HttpServer;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判题客户端与沙箱协议的契约测试（离线，不依赖 Docker）。
 *
 * <p>重点锁定三条安全约束：</p>
 * <ul>
 *   <li>请求体<b>不得</b>包含 {@code expectedOutput}（标准答案不离开后端）；</li>
 *   <li>未配置 {@code judge.token} 时 fail-closed，不发起任何请求；</li>
 *   <li>沙箱异常/非 200/code!=200 都收敛为"未执行"，不会误判为通过。</li>
 * </ul>
 */
class JudgeClientTest {

    private HttpServer server;

    private final AtomicReference<String> lastBody = new AtomicReference<>();

    private final AtomicReference<String> lastToken = new AtomicReference<>();

    private volatile int responseStatus = 200;

    private volatile String responseBody = "{\"requestId\":\"r1\",\"status\":\"success\",\"code\":200,"
            + "\"message\":\"judged\",\"verdict\":\"EXECUTED\",\"cases\":["
            + "{\"index\":0,\"verdict\":\"ACCEPTED\",\"stdout\":\"3\\n\",\"timeMs\":12,\"memoryBytes\":2048}]}";

    @BeforeEach
    void start() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/v1/judge", exchange -> {
            lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
            lastToken.set(exchange.getRequestHeaders().getFirst("X-Judge-Token"));
            byte[] body = responseBody.getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(responseStatus, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stop() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void requestNeverCarriesExpectedOutput() {
        JudgeClient client = client(2000, "secret-token");
        JudgeClient.JudgeResult result = client.judge("commit-1-abc", "c++", "int main(){}",
                Arrays.asList(new TestCase("1 2\n", "SUPER_SECRET_ANSWER")), 1000L, 256L);

        assertTrue(result.isExecuted());
        assertEquals("secret-token", lastToken.get());
        String body = lastBody.get();
        assertFalse(body.contains("expectedOutput"), "标准答案不应下发到沙箱: " + body);
        assertFalse(body.contains("SUPER_SECRET_ANSWER"));
        assertTrue(body.contains("\"input\":\"1 2\\n\""));
        assertEquals(1, result.getCases().size());
        assertEquals(12L, result.getCases().get(0).getTimeMs());
    }

    @Test
    void failsClosedWhenTokenMissing() {
        JudgeClient client = client(2000, "");
        JudgeClient.JudgeResult result = client.judge("r", "c++", "code",
                Collections.singletonList(new TestCase("1", "1")), 1000L, 256L);

        assertFalse(result.isExecuted());
        assertFalse(result.isCompileError());
        assertEquals(null, lastBody.get(), "未配置令牌时不应发出请求");
    }

    @Test
    void compileErrorIsReported() {
        responseBody = "{\"requestId\":\"r\",\"code\":200,\"verdict\":\"COMPILE_ERROR\","
                + "\"compileOutput\":\"main.cpp:1: error\"}";
        JudgeClient.JudgeResult result = client(2000, "t").judge("r", "c++", "bad",
                Collections.singletonList(new TestCase("1", "1")), 1000L, 256L);

        assertFalse(result.isExecuted());
        assertTrue(result.isCompileError());
        assertTrue(result.getCompileOutput().contains("error"));
    }

    @Test
    void httpErrorIsNotTreatedAsSuccess() {
        responseStatus = 503;
        responseBody = "{\"code\":503,\"message\":\"鉴权失败\"}";
        JudgeClient.JudgeResult result = client(2000, "t").judge("r", "c++", "code",
                Collections.singletonList(new TestCase("1", "1")), 1000L, 256L);

        assertFalse(result.isExecuted());
        assertFalse(result.isCompileError());
    }

    @Test
    void businessErrorCodeIsNotTreatedAsSuccess() {
        responseBody = "{\"requestId\":\"r\",\"code\":400,\"message\":\"至少需要一个测试点\"}";
        JudgeClient.JudgeResult result = client(2000, "t").judge("r", "c++", "code",
                Collections.singletonList(new TestCase("1", "1")), 1000L, 256L);

        assertFalse(result.isExecuted());
        assertFalse(result.isCompileError());
        assertTrue(result.getMessage().contains("至少需要一个测试点"));
    }

    @Test
    void missingTimingFieldsAreKeptNull() {
        responseBody = "{\"requestId\":\"r\",\"code\":200,\"verdict\":\"EXECUTED\",\"cases\":["
                + "{\"index\":0,\"verdict\":\"ACCEPTED\",\"stdout\":\"1\"}]}";
        JudgeClient.JudgeResult result = client(2000, "t").judge("r", "c++", "code",
                Collections.singletonList(new TestCase("1", "1")), 1000L, 256L);

        assertTrue(result.isExecuted());
        assertEquals(null, result.getCases().get(0).getTimeMs());
        assertEquals(null, result.getCases().get(0).getMemoryBytes());
    }

    private JudgeClient client(long readTimeout, String token) {
        return new JudgeClient("http://127.0.0.1:" + server.getAddress().getPort() + "/api/v1/judge",
                token, 2000L, readTimeout);
    }
}
