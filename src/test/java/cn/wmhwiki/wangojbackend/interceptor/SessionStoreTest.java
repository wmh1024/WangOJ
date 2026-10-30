package cn.wmhwiki.wangojbackend.interceptor;

import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import cn.wmhwiki.wangojbackend.constant.RedisTimeoutConstant;
import cn.wmhwiki.wangojbackend.util.TokenUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;

import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

/**
 * 会话索引 TTL 单测：确保每次请求同时续期 token 与 user_tokens。
 */
@ExtendWith(MockitoExtension.class)
class SessionStoreTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private SetOperations<String, String> setOperations;

    private SessionStore sessionStore;

    @BeforeEach
    void setUp() {
        // 不同用例使用不同分支，使用 lenient 避免严格 stub 的“未使用”告警
        org.mockito.Mockito.lenient().when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        org.mockito.Mockito.lenient().when(stringRedisTemplate.opsForSet()).thenReturn(setOperations);
        sessionStore = new SessionStore(stringRedisTemplate, new RedisTimeoutConstant(10L, 12L, 5L));
    }

    @Test
    void touchRefreshesTokenAndUserTokenSet() {
        sessionStore.touch("abc-token", 7L);

        verify(stringRedisTemplate).expire("wangoj:token:abc-token", 10L, TimeUnit.MINUTES);
        verify(setOperations).add("wangoj:user_tokens:7", "abc-token");
        verify(stringRedisTemplate).expire("wangoj:user_tokens:7", 20L, TimeUnit.MINUTES);
    }

    @Test
    void issueIndexesTokenForUser() {
        String bearer = sessionStore.issue(7L);
        String token = bearer.substring("Bearer ".length());

        ArgumentCaptor<String> tokenKey = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(tokenKey.capture(), eq("7"), eq(10L), eq(TimeUnit.MINUTES));
        assertEquals("wangoj:token:" + token, tokenKey.getValue());
        verify(setOperations).add("wangoj:user_tokens:7", token);
        verify(stringRedisTemplate).expire("wangoj:user_tokens:7", 20L, TimeUnit.MINUTES);
    }

    @Test
    void touchIgnoresMissingTokenOrUser() {
        sessionStore.touch(null, 7L);
        sessionStore.touch("token", null);

        verify(stringRedisTemplate, never()).expire(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.any(TimeUnit.class));
    }

    /**
     * 回归测试：写入 Redis 的 key 必须是**归一化**后的 token。
     *
     * <p>请求侧 {@code TokenUtils.resolveToken} 会把 {@code Bearer} 后的 uuid 去掉连字符再查，
     * 所以这里若存 {@code UUID.toString()} 的带连字符形态，登录会成功但后续请求全部 401。
     * 该用例在修复前必然失败。</p>
     */
    @Test
    void issuedTokenIsStoredInNormalizedForm() {
        String bearer = sessionStore.issue(7L);
        String token = bearer.substring("Bearer ".length());

        assertEquals(32, token.length(), "token 应为 32 位十六进制");
        assertEquals(token.toLowerCase(), token, "token 应全为小写");
        assertEquals(-1, token.indexOf('-'), "token 不应包含连字符");

        ArgumentCaptor<String> tokenKey = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(tokenKey.capture(), eq("7"), eq(10L), eq(TimeUnit.MINUTES));
        assertEquals("wangoj:token:" + token, tokenKey.getValue());
    }

    /** 回归测试：请求侧归一化后的 token 必须能命中 issue() 写入的同一个 Redis key。 */
    @Test
    void resolvedTokenMatchesIssuedTokenKey() {
        String bearer = sessionStore.issue(7L);

        ArgumentCaptor<String> tokenKey = ArgumentCaptor.forClass(String.class);
        verify(valueOperations).set(tokenKey.capture(), eq("7"), eq(10L), eq(TimeUnit.MINUTES));

        // 模拟真实请求链路：把 token 放回 Authorization，再按请求侧逻辑解析
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("Authorization", bearer);
        String resolved = TokenUtils.resolveToken(request);

        assertEquals(tokenKey.getValue(), RedisKeyConstant.TOKEN + resolved,
                "请求侧解析出的 token 必须与写入 Redis 的 key 一致");
    }
}
