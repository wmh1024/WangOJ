package cn.wmhwiki.wangojbackend.config;

import cn.wmhwiki.wangojbackend.exception.BusinessException;
import cn.wmhwiki.wangojbackend.service.impl.AIServiceImpl;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import cn.wmhwiki.wangojbackend.vo.ProblemVO;
import cn.wmhwiki.wangojbackend.service.ProblemService;
import org.mockito.Mockito;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * AI 客户端的离线单元测试。
 *
 * <p>用 JDK 自带 HttpServer 模拟 OpenAI 兼容接口，不访问外网、不依赖 Spring 容器。</p>
 */
class AiPropertiesTest {

    private HttpServer server;
    private volatile String lastRequestBody;

    @BeforeEach
    void startServer() throws Exception {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/v1/chat/completions", exchange -> {
            lastRequestBody = new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            byte[] body = ("{\"choices\":[{\"message\":{\"role\":\"assistant\",\"content\":\"ok\"}}]}")
                    .getBytes(StandardCharsets.UTF_8);
            exchange.getResponseHeaders().add("Content-Type", "application/json");
            exchange.sendResponseHeaders(200, body.length);
            try (OutputStream os = exchange.getResponseBody()) {
                os.write(body);
            }
        });
        server.start();
    }

    @AfterEach
    void stopServer() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    void urlIsNormalized() {
        assertUrl("https://api.openai.com", "https://api.openai.com/v1/chat/completions");
        assertUrl("https://api.openai.com/", "https://api.openai.com/v1/chat/completions");
        assertUrl("https://gateway.example.com/v1", "https://gateway.example.com/v1/chat/completions");
        assertUrl("https://gateway.example.com/v1/chat/completions",
                "https://gateway.example.com/v1/chat/completions");
    }

    @Test
    void unusableWhenDisabledOrWithoutKey() {
        AiProperties properties = new AiProperties();
        assertTrue(!properties.isUsable());

        properties.setEnabled(true);
        assertTrue(!properties.isUsable());

        properties.setApiKey("  ");
        assertTrue(!properties.isUsable());

        properties.setApiKey("key");
        assertTrue(properties.isUsable());
    }

    @Test
    void chatParsesFirstChoice() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setReadTimeoutMs(5000);

        // 只验证 HTTP 调用与响应解析，题目内容用 Mock 提供
        ProblemService problemService = Mockito.mock(ProblemService.class);
        ProblemVO problemVO = new ProblemVO();
        problemVO.setContent("题目");
        Mockito.when(problemService.getProblemContent(1L)).thenReturn(problemVO);

        AIServiceImpl service = new AIServiceImpl();
        ReflectionTestUtils.setField(service, "aiProperties", properties);
        ReflectionTestUtils.setField(service, "problemService", problemService);

        String answer = service.codeExplain("int main(){}", 1L, "帮我加注释");
        assertEquals("ok", answer);
        assertTrue(lastRequestBody != null && lastRequestBody.contains("\"messages\""));
        assertTrue(lastRequestBody.contains("帮我加注释"));
    }

    @Test
    void rejectsMissingProblemId() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());

        AIServiceImpl service = new AIServiceImpl();
        ReflectionTestUtils.setField(service, "aiProperties", properties);
        ReflectionTestUtils.setField(service, "problemService", Mockito.mock(ProblemService.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.codeExplain("int main(){}", null, "解释"));
        assertTrue(error.getDescription().contains("题目"));
    }

    @Test
    void rejectsTooLongCodeBeforeCallingUpstream() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(true);
        properties.setApiKey("test-key");
        properties.setBaseUrl("http://127.0.0.1:" + server.getAddress().getPort());
        properties.setMaxCodeLength(10);

        AIServiceImpl service = new AIServiceImpl();
        ReflectionTestUtils.setField(service, "aiProperties", properties);
        ReflectionTestUtils.setField(service, "problemService", Mockito.mock(ProblemService.class));

        assertThrows(BusinessException.class,
                () -> service.codeExplain("01234567890123456789", 1L, null));
    }

    @Test
    void rejectsWhenAiDisabled() {
        AiProperties properties = new AiProperties();
        properties.setEnabled(false);

        AIServiceImpl service = new AIServiceImpl();
        ReflectionTestUtils.setField(service, "aiProperties", properties);
        ReflectionTestUtils.setField(service, "problemService", Mockito.mock(ProblemService.class));

        BusinessException error = assertThrows(BusinessException.class,
                () -> service.codeExplain("int main(){}", 1L, null));
        assertTrue(error.getDescription().contains("未启用"));
    }

    private static void assertUrl(String baseUrl, String expected) {
        AiProperties properties = new AiProperties();
        properties.setBaseUrl(baseUrl);
        assertEquals(expected, properties.chatCompletionsUrl());
    }
}
