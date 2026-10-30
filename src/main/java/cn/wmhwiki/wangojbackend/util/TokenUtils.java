package cn.wmhwiki.wangojbackend.util;

import jakarta.servlet.http.HttpServletRequest;

/**
 * 从 HTTP 请求解析 {@code Authorization: Bearer <uuid>}。
 *
 * <p>只接受 {@code Bearer <uuid>} 形态，且 uuid 仅允许十六进制与连字符，
 * 避免把任意字符串拼进 Redis key。实现委托给 {@link StringUtils}，保持单一事实来源。</p>
 */
public final class TokenUtils {

    private TokenUtils() {
    }

    /**
     * 从指定请求中解析 Token。
     *
     * @param request HTTP 请求
     * @return 去掉前缀的 uuid；缺失或格式错误返回 null
     */
    public static String resolveToken(HttpServletRequest request) {
        if (request == null) {
            return null;
        }
        return StringUtils.resolveBearerToken(request.getHeader("Authorization"));
    }

    /**
     * 从当前 Web 请求中解析 Token（拦截器/Controller/Service 内可直接调用）。
     */
    public static String resolveCurrentToken() {
        return StringUtils.resolveCurrentToken();
    }

    /**
     * 校验并归一化 uuid（去掉连字符、转小写）。
     *
     * @return 合法时返回 32 位小写十六进制；非法返回 null
     */
    public static String normalize(String uuid) {
        return StringUtils.normalizeToken(uuid);
    }
}
