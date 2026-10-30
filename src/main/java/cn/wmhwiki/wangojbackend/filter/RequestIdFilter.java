package cn.wmhwiki.wangojbackend.filter;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.MDC;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

/**
 * 请求 ID 过滤器。
 *
 * <p>为每个进入后端的请求生成（或沿用调用方传入的）请求 ID，写入：</p>
 * <ul>
 *   <li>MDC 的 {@code requestId}，日志格式中输出，便于串联一次请求的全部日志；</li>
 *   <li>响应头 {@code X-Request-Id}，前端报错时可带上，方便定位。</li>
 * </ul>
 *
 * <p>判题线程会复制该 ID，并在调用沙箱时作为协议字段 {@code requestId} 的前缀来源。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
public class RequestIdFilter extends OncePerRequestFilter {

    public static final String HEADER = "X-Request-Id";

    public static final String MDC_KEY = "requestId";

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String requestId = sanitize(request.getHeader(HEADER));
        if (requestId == null) {
            requestId = UUID.randomUUID().toString().replace("-", "");
        }
        MDC.put(MDC_KEY, requestId);
        response.setHeader(HEADER, requestId);
        try {
            chain.doFilter(request, response);
        } finally {
            MDC.remove(MDC_KEY);
        }
    }

    /**
     * 只接受安全字符，避免把任意输入写进日志/响应头。
     */
    static String sanitize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty() || trimmed.length() > 64 || !trimmed.matches("[A-Za-z0-9_.:-]+")) {
            return null;
        }
        return trimmed;
    }
}
