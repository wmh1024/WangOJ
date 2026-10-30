package cn.wmhwiki.wangojbackend.util;

import cn.hutool.core.util.StrUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * 字符串与请求头工具。
 */
public final class StringUtils {

    private static final String HEADER = "Authorization";

    private static final String BEARER_PREFIX = "bearer";

    /**
     * Token 只接受 UUID 形态（去掉连字符后 32 位十六进制），杜绝任意字符串进入 Redis key
     */
    private static final String UUID_PATTERN = "^[0-9a-f]{32}$";

    private static final int UUID_LENGTH = 32;

    private StringUtils() {
    }

    /**
     * 把任意字符串按 UUID 形态归一化（去连字符、转小写）。
     *
     * @return 合法时返回 32 位小写十六进制；非法返回 null
     */
    public static String normalizeToken(String uuid) {
        if (StrUtil.isBlank(uuid)) {
            return null;
        }
        String compact = uuid.trim().replace("-", "").toLowerCase();
        if (compact.length() != UUID_LENGTH || !compact.matches(UUID_PATTERN)) {
            return null;
        }
        return compact;
    }

    /**
     * 从 {@code Bearer <uuid>} 字符串中解析并归一化 Token。
     *
     * <p>只接受两段式（{@code Bearer} + uuid）。裸 uuid 不接受，避免把任意请求头
     * 当作凭证，也避免 {@code Basic xxx} 之类的认证头被误解析。</p>
     */
    public static String resolveBearerToken(String authorization) {
        if (StrUtil.isBlank(authorization)) {
            return null;
        }
        String[] parts = authorization.trim().split("\\s+");
        if (parts.length != 2 || !BEARER_PREFIX.equalsIgnoreCase(parts[0])) {
            // 只接受 "Bearer <uuid>" 两段式：Basic、裸 uuid、含多余空白的都拒绝
            return null;
        }
        return normalizeToken(parts[1]);
    }

    /**
     * 解析当前请求头中的 Token。
     *
     * @return 去掉前缀的 uuid；缺失或格式错误返回 null
     */
    public static String resolveCurrentToken() {
        ServletRequestAttributes attributes =
                (ServletRequestAttributes) RequestContextHolder.getRequestAttributes();
        if (attributes == null) {
            return null;
        }
        HttpServletRequest request = attributes.getRequest();
        return resolveBearerToken(request.getHeader(HEADER));
    }
}
