package cn.wmhwiki.wangojbackend.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * AI 功能配置。
 *
 * <p>全部通过环境变量注入，详见 `.env.example`。</p>
 *
 * <pre>
 * app:
 *   ai:
 *     enabled: ${APP_AI_ENABLED:false}
 *     base-url: ${AI_BASE_URL:https://api.openai.com}
 *     api-key: ${AI_API_KEY:}
 *     model: ${AI_MODEL:gpt-4o-mini}
 *     connect-timeout-ms: 5000
 *     read-timeout-ms: 60000
 *     max-code-length: 65536
 * </pre>
 *
 * @author WangOJ contributors
 */
@Component
@ConfigurationProperties(prefix = "app.ai")
@Getter
@Setter
public class AiProperties {

    /**
     * 是否启用 AI 功能；关闭时接口返回明确提示，不会发起任何外部请求
     */
    private boolean enabled = false;

    /**
     * OpenAI 兼容接口地址，例如 https://api.openai.com 或自建网关
     */
    private String baseUrl = "https://api.openai.com";

    /**
     * API Key，必须通过环境变量注入
     */
    private String apiKey = "";

    /**
     * 模型名
     */
    private String model = "gpt-4o-mini";

    private long connectTimeoutMs = 5000L;

    private long readTimeoutMs = 60000L;

    /**
     * 单次请求允许的最大代码长度
     */
    private int maxCodeLength = 64 * 1024;

    /**
     * 配置是否完整可用。
     */
    public boolean isUsable() {
        return enabled && apiKey != null && !apiKey.trim().isEmpty()
                && baseUrl != null && !baseUrl.trim().isEmpty();
    }

    /**
     * 拼接 chat/completions 地址，兼容 base-url 带或不带 /v1。
     */
    public String chatCompletionsUrl() {
        String base = baseUrl.trim();
        while (base.endsWith("/")) {
            base = base.substring(0, base.length() - 1);
        }
        if (base.endsWith("/v1") || base.endsWith("/chat/completions")) {
            return base.endsWith("/chat/completions") ? base : base + "/chat/completions";
        }
        return base + "/v1/chat/completions";
    }
}
