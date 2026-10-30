package cn.wmhwiki.wangojbackend.service.impl;

import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

/**
 * 登录失败限流。
 *
 * <p>按账号计数：窗口期内失败次数达到阈值后，在锁定期内拒绝该账号继续尝试登录。
 * 计数存于 Redis，多实例部署时自动共享。</p>
 *
 * @author WangOJ contributors
 */
@Slf4j
@Component
public class LoginAttemptGuard {

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    /**
     * 允许的最大连续失败次数
     */
    @Value("${app.login-max-failures:5}")
    private int maxFailures;

    /**
     * 失败计数窗口与锁定时间（秒）
     */
    @Value("${app.login-lock-seconds:300}")
    private long lockSeconds;

    /**
     * 该账号当前是否被锁定。
     */
    public boolean isLocked(String account) {
        String key = RedisKeyConstant.LOGIN_FAIL + normalize(account);
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            return value != null && parseInt(value) >= maxFailures;
        } catch (Exception e) {
            // 与 recordFailure/reset 保持一致：Redis 异常不阻断登录
            log.warn("读取登录锁定状态失败: {}", e.getMessage());
            return false;
        }
    }

    /**
     * 剩余锁定秒数（用于提示）。
     */
    public long remainingSeconds(String account) {
        String key = RedisKeyConstant.LOGIN_FAIL + normalize(account);
        try {
            Long ttl = stringRedisTemplate.getExpire(key, TimeUnit.SECONDS);
            return ttl == null || ttl < 0 ? lockSeconds : ttl;
        } catch (Exception e) {
            log.warn("读取登录锁定时间失败: {}", e.getMessage());
            return lockSeconds;
        }
    }

    /**
     * 记录一次失败；达到阈值后开始锁定。
     */
    public void recordFailure(String account) {
        String key = RedisKeyConstant.LOGIN_FAIL + normalize(account);
        try {
            Long count = stringRedisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                stringRedisTemplate.expire(key, lockSeconds, TimeUnit.SECONDS);
            }
            // 达到阈值时重新计时，保证锁定时间从最后一次失败开始算
            if (count != null && count >= maxFailures) {
                stringRedisTemplate.expire(key, lockSeconds, TimeUnit.SECONDS);
                log.warn("账号登录失败次数过多，已锁定 {} 秒", lockSeconds);
            }
        } catch (Exception e) {
            // Redis 异常不应阻断登录流程，只记录日志
            log.warn("记录登录失败次数失败: {}", e.getMessage());
        }
    }

    /**
     * 登录成功，清除计数。
     */
    public void reset(String account) {
        try {
            stringRedisTemplate.delete(RedisKeyConstant.LOGIN_FAIL + normalize(account));
        } catch (Exception e) {
            log.warn("清除登录失败计数失败: {}", e.getMessage());
        }
    }

    /**
     * 账号归一化，避免大小写/空白绕过计数。
     */
    private static String normalize(String account) {
        return account == null ? "" : account.trim().toLowerCase();
    }

    private static int parseInt(String value) {
        try {
            return Integer.parseInt(value);
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
