package cn.wmhwiki.wangojbackend.exception;

import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 业务异常：预期内的、可展示给用户的错误。
 *
 * <p>可选择携带 HTTP 状态码（默认 400）。由 {@link GlobalExceptionHandler} 转换。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class BusinessException extends RuntimeException {

    /**
     * 建议的 HTTP 状态码
     */
    private final int status;

    public BusinessException(String description) {
        this(400, description);
    }

    public BusinessException(int status, String description) {
        super(description);
        this.status = status;
    }

    public String getDescription() {
        return getMessage();
    }

    /**
     * 未登录 / Token 失效
     */
    public static BusinessException unauthorized(String description) {
        return new BusinessException(401, description);
    }

    /**
     * 无权限
     */
    public static BusinessException forbidden(String description) {
        return new BusinessException(403, description);
    }

    /**
     * 资源不存在
     */
    public static BusinessException notFound(String description) {
        return new BusinessException(404, description);
    }

    public static BusinessException tooManyRequests(String description) {
        return new BusinessException(429, description);
    }
}
