package cn.wmhwiki.wangojbackend.config;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 跨域来源解析测试：未配置时不得默认放开跨域。
 */
class CorsResolverTest {

    @Test
    void corsOriginsAreTrimmedAndEmptyIgnored() {
        CorsConfig config = new CorsConfig(" http://localhost:5173 , ,https://oj.example.com ");
        assertEquals(2, config.allowedOrigins().length);
        assertEquals("http://localhost:5173", config.allowedOrigins()[0]);
        assertEquals("https://oj.example.com", config.allowedOrigins()[1]);
    }

    @Test
    void emptyCorsConfigDisablesCors() {
        assertEquals(0, new CorsConfig("").allowedOrigins().length);
        assertEquals(0, new CorsConfig(null).allowedOrigins().length);
    }
}
