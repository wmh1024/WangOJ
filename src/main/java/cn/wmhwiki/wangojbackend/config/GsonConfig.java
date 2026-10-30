package cn.wmhwiki.wangojbackend.config;

import com.google.gson.Gson;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Gson配置类
 *
 * @author WangOJ contributors
 */
@Configuration
public class GsonConfig {
    @Bean
    public Gson gson() {
        return new Gson();
    }
}
