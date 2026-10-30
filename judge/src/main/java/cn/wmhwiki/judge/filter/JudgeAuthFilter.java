/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.filter;

import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import com.google.gson.Gson;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 判题接口鉴权。
 *
 * <p>后端与沙箱共享一个密钥 {@code X-Judge-Token}，通过环境变量注入。
 * 未配置密钥时拒绝所有判题请求（fail-closed），避免“忘配密钥导致裸奔”。</p>
 *
 * <p>健康检查 {@code /ping} 不需要鉴权。</p>
 */
@Component
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class JudgeAuthFilter extends OncePerRequestFilter {

    /**
     * 共享密钥请求头
     */
    public static final String TOKEN_HEADER = "X-Judge-Token";

    /**
     * 需要鉴权的路径前缀
     */
    private static final String JUDGE_PATH = "/api/v1/judge";

    private final Gson gson = new Gson();

    @Value("${judge.token:}")
    private String expectedToken;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        if (!isProtected(request)) {
            chain.doFilter(request, response);
            return;
        }
        String actualToken = request.getHeader(TOKEN_HEADER);
        if (isTokenBlank() || actualToken == null || !constantTimeEquals(expectedToken, actualToken)) {
            writeError(response, actualToken == null ? "缺少鉴权令牌" : "鉴权失败");
            return;
        }
        chain.doFilter(request, response);
    }

    private boolean isProtected(HttpServletRequest request) {
        return request.getRequestURI().startsWith(JUDGE_PATH);
    }

    /**
     * 未配置、空白或仍是示例占位值时都视为不可用，统一 fail-closed。
     * 这样可以避免用户直接复用 .env.example 的 __REQUIRED__ 造成“两端一致但等于公开密钥”。
     */
    private boolean isTokenBlank() {
        if (expectedToken == null) {
            return true;
        }
        String token = expectedToken.trim();
        return token.isEmpty() || "__REQUIRED__".equals(token)
                || "please-generate-a-random-token".equals(token);
    }

    /**
     * 恒定时间比较，避免通过响应时间猜测令牌。
     */
    private static boolean constantTimeEquals(String expected, String actual) {
        byte[] left = expected.getBytes(StandardCharsets.UTF_8);
        byte[] right = actual.getBytes(StandardCharsets.UTF_8);
        return MessageDigest.isEqual(left, right);
    }

    private void writeError(HttpServletResponse response, String message) throws IOException {
        if (isTokenBlank()) {
            // 服务端未配置密钥属于部署错误，返回 503 更贴近事实
            response.setStatus(HttpServletResponse.SC_SERVICE_UNAVAILABLE);
        } else {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        }
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        response.getWriter().write(gson.toJson(JudgeResponse.error(null, response.getStatus(), message)));
    }
}
