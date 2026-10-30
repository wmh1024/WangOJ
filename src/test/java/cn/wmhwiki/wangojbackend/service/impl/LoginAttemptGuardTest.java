package cn.wmhwiki.wangojbackend.service.impl;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

/**
 * 登录锁定读取路径在 Redis 故障时应 fail-open。
 */
@ExtendWith(MockitoExtension.class)
class LoginAttemptGuardTest {

    @Mock
    private StringRedisTemplate stringRedisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void redisFailureDoesNotBlockLogin() {
        LoginAttemptGuard guard = new LoginAttemptGuard();
        ReflectionTestUtils.setField(guard, "stringRedisTemplate", stringRedisTemplate);
        ReflectionTestUtils.setField(guard, "maxFailures", 5);
        ReflectionTestUtils.setField(guard, "lockSeconds", 300L);

        when(stringRedisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("wangoj:login_fail:alice"))
                .thenThrow(new RedisConnectionFailureException("down"));
        when(stringRedisTemplate.getExpire("wangoj:login_fail:alice", java.util.concurrent.TimeUnit.SECONDS))
                .thenThrow(new RedisConnectionFailureException("down"));

        assertFalse(guard.isLocked("alice"));
        assertEquals(300L, guard.remainingSeconds("alice"));
    }
}
