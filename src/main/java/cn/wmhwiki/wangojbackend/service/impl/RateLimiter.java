package cn.wmhwiki.wangojbackend.service.impl;

import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import jakarta.annotation.Resource;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 简单频率限制：固定窗口计数。
 *
 * <p>用于抑制刷接口（例如反复调用 AI 接口耗尽额度）。
 * Redis 不可用时**放行**（fail-open），避免缓存故障导致正常用户无法使用。</p>
 *
 * @author WangOJ contributors
 */
@Component
public class RateLimiter {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 单个用户每分钟最多调用 AI 接口的次数
     */
    @Value("${app.ai.rate-limit-per-minute:10}")
    private int aiLimitPerMinute;

    /**
     * AI 接口限流。
     *
     * @param userId 用户 ID
     * @return 是否允许本次调用
     */
    public boolean allowAiCall(Long userId) {
        return allow(RedisKeyConstant.AI_RATE_LIMIT + userId, aiLimitPerMinute, 60);
    }

    /**
     * 固定窗口计数。
     */
    private boolean allow(String key, int limit, long windowSeconds) {
        if (limit <= 0) {
            return true;
        }
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, windowSeconds, TimeUnit.SECONDS);
            }
            return count == null || count <= limit;
        } catch (Exception e) {
            return true;
        }
    }
}
