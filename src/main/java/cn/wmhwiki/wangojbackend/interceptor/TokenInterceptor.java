package cn.wmhwiki.wangojbackend.interceptor;

import cn.hutool.core.util.StrUtil;
import cn.wmhwiki.wangojbackend.constant.RedisKeyConstant;
import cn.wmhwiki.wangojbackend.domain.User;
import cn.wmhwiki.wangojbackend.service.UserService;
import cn.wmhwiki.wangojbackend.util.TokenUtils;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * Token 拦截器。
 *
 * <p>解析 {@code Authorization: Bearer <uuid>}：Redis 中只保存 {@code token -> userId}，
 * 用户信息（尤其是 {@code userRole}）<b>每次请求都回源数据库</b>，保证改密、降权、删号
 * 在 Token 有效期内立即生效。未拿到有效用户时不抛异常，交由后续登录/管理员拦截器决定是否放行。</p>
 *
 * <p>每次请求都会刷新 Token 有效期（滑动过期）。Redis 异常时按未登录处理，不阻断请求链。</p>
 */
@Slf4j
public class TokenInterceptor implements HandlerInterceptor {

    private final StringRedisTemplate stringRedisTemplate;

    private final UserService userService;

    private final SessionStore sessionStore;

    public TokenInterceptor(StringRedisTemplate stringRedisTemplate,
                            UserService userService,
                            SessionStore sessionStore) {
        this.stringRedisTemplate = stringRedisTemplate;
        this.userService = userService;
        this.sessionStore = sessionStore;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String token = TokenUtils.resolveToken(request);
        if (token == null) {
            return true;
        }
        String tokenKey = RedisKeyConstant.TOKEN + token;
        String userIdValue;
        try {
            userIdValue = stringRedisTemplate.opsForValue().get(tokenKey);
        } catch (Exception e) {
            // Redis 故障时按未登录处理，不阻断请求链
            log.warn("读取 Token 失败: {}", e.getMessage());
            return true;
        }
        if (StrUtil.isBlank(userIdValue)) {
            return true;
        }
        Long userId;
        try {
            userId = Long.valueOf(userIdValue.trim());
        } catch (NumberFormatException e) {
            log.warn("Token 对应的用户 ID 非法，已忽略");
            return true;
        }
        User user;
        try {
            user = userService.getById(userId);
        } catch (Exception e) {
            log.warn("回源用户信息失败, userId={}: {}", userId, e.getMessage());
            return true;
        }
        if (user == null) {
            // 账号已删除：立即失效，避免旧 Token 继续可用，同时清掉用户 Token 索引
            revokeQuietly(token);
            return true;
        }
        UserHolder.saveUserVO(UserHolder.toUserVO(user));
        try {
            // 同时续期 token 与 user_tokens 集合，保证改密/降权时仍能批量吊销
            sessionStore.touch(token, userId);
        } catch (Exception e) {
            log.warn("刷新 Token 有效期失败: {}", e.getMessage());
        }
        return true;
    }

    @Override
    public void afterCompletion(HttpServletRequest request, HttpServletResponse response, Object handler,
                                Exception ex) {
        UserHolder.removeUserVO();
    }

    private void revokeQuietly(String token) {
        try {
            sessionStore.revoke(token);
        } catch (Exception e) {
            log.warn("清理失效 Token 失败: {}", e.getMessage());
        }
    }
}
