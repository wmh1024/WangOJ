package cn.wmhwiki.wangojbackend.util;

import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Token 解析的边界测试。
 *
 * <p>Token 会被直接拼接到 Redis key 中，因此必须严格限制形态，不能接受任意字符串。</p>
 */
class TokenUtilsTest {

    private static final String VALID_UUID = "3f6b0d9e-1a2b-4c3d-8e9f-0a1b2c3d4e5f";

    @Test
    void resolvesBearerToken() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "Bearer " + VALID_UUID);
        assertEquals("3f6b0d9e1a2b4c3d8e9f0a1b2c3d4e5f", TokenUtils.resolveToken(request));
    }

    @Test
    void acceptsLowercaseAndExtraSpaces() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", "  bearer    " + VALID_UUID.toUpperCase() + "  ");
        assertEquals("3f6b0d9e1a2b4c3d8e9f0a1b2c3d4e5f", TokenUtils.resolveToken(request));
    }

    @Test
    void rejectsMalformedHeaders() {
        assertNull(TokenUtils.resolveToken(request(null)));
        assertNull(TokenUtils.resolveToken(request("")));
        assertNull(TokenUtils.resolveToken(request("Bearer")));
        assertNull(TokenUtils.resolveToken(request("Basic " + VALID_UUID)));
        assertNull(TokenUtils.resolveToken(request("Bearer abc def")));
        // 非十六进制字符不可进入 Redis key
        assertNull(TokenUtils.resolveToken(request("Bearer ../../etc/passwd")));
        assertNull(TokenUtils.resolveToken(request("Bearer " + "z".repeat(32))));
        // 长度不足 32 位十六进制
        assertNull(TokenUtils.resolveToken(request("Bearer abcdef")));
        assertNull(TokenUtils.resolveToken(null));
    }

    @Test
    void normalizeStripsDashesAndLowercases() {
        assertEquals("3f6b0d9e1a2b4c3d8e9f0a1b2c3d4e5f",
                TokenUtils.normalize("3F6B0D9E-1A2B-4C3D-8E9F-0A1B2C3D4E5F"));
        assertNull(TokenUtils.normalize("   "));
        assertNull(TokenUtils.normalize(null));
    }

    private static MockHttpServletRequest request(String authorization) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        if (authorization != null) {
            request.addHeader("Authorization", authorization);
        }
        return request;
    }
}
