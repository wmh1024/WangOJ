package cn.wmhwiki.wangojbackend.config;

import cn.wmhwiki.wangojbackend.interceptor.AdminInterceptor;
import cn.wmhwiki.wangojbackend.interceptor.LoginInterceptor;
import cn.wmhwiki.wangojbackend.interceptor.SessionStore;
import cn.wmhwiki.wangojbackend.interceptor.TokenInterceptor;
import cn.wmhwiki.wangojbackend.service.UserService;
import jakarta.annotation.Resource;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

/**
 * MVC配置类
 *
 * @author WangOJ contributors
 */
@Configuration
public class MvcConfig implements WebMvcConfigurer {

    /**
     * 管理接口前缀（需要 admin 角色）
     */
    private static final String ADMIN_PATH = "/api/admin/**";

    @Resource
    private StringRedisTemplate stringRedisTemplate;

    @Resource
    private UserService userService;

    @Resource
    private SessionStore sessionStore;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        // 1、先执行 token 拦截器，拦截所有路径。解析 Token 并回源用户信息，写入 ThreadLocal
        registry.addInterceptor(new TokenInterceptor(stringRedisTemplate, userService, sessionStore))
                .addPathPatterns("/**")
                .order(0);
        // 2、后执行登录拦截器，将未登录不能访问的路径拦截
        registry.addInterceptor(new LoginInterceptor())
                .addPathPatterns("/api/commit/code", "/api/ai/**", "/api/user/logout")
                .order(2);
        // 3、管理员拦截器，将非管理员不能访问的路径拦截
        registry.addInterceptor(new AdminInterceptor())
                .addPathPatterns(ADMIN_PATH)
                .order(4);
    }
}
