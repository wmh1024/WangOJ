package cn.wmhwiki.wangojbackend.config;

import cn.hutool.core.util.StrUtil;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Arrays;
import java.util.List;

/**
 * 解决跨域问题。
 *
 * <p>只允许配置中列出的来源（{@code app.cors-origins}，逗号分隔），
 * 不再使用 {@code allowedOriginPatterns("*")} + credentials 的组合。</p>
 *
 * @author WangOJ contributors
 */
@Configuration
public class CorsConfig implements WebMvcConfigurer {

    /**
     * 当前跨域请求最大有效时长。这里默认1天
     */
    private static final long MAX_AGE = 24 * 60 * 60;

    private final String[] allowedOrigins;

    public CorsConfig(@Value("${app.cors-origins:}") String corsOrigins) {
        List<String> origins = Arrays.stream(StrUtil.nullToEmpty(corsOrigins).split(","))
                .map(String::trim)
                .filter(origin -> !origin.isEmpty())
                .toList();
        this.allowedOrigins = origins.toArray(new String[0]);
    }

    /**
     * 已解析的允许来源（仅用于测试与排查，避免把解析逻辑散落在测试里）。
     */
    String[] allowedOrigins() {
        return allowedOrigins.clone();
    }

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        // 未配置来源时不开放任何跨域，避免默认全开
        if (allowedOrigins.length == 0) {
            return;
        }
        registry.addMapping("/**")
                .allowCredentials(true)
                .allowedOrigins(allowedOrigins)
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .exposedHeaders("Authorization")
                .maxAge(MAX_AGE);
    }
}
