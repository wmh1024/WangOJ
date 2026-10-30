package cn.wmhwiki.wangojbackend.exception;

import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.stream.Collectors;

/**
 * 全局异常处理。
 *
 * <p>约定：所有响应都使用 {@code {code, msg, data}} 结构，且 HTTP 状态码与 {@code code} 一致。</p>
 * <ul>
 *   <li>业务异常：按异常携带的 {@code status}（默认 400）；</li>
 *   <li>参数异常：400；</li>
 *   <li>路由不存在 / 静态资源不存在：404；</li>
 *   <li>其他异常：500，且不把堆栈或内部信息返回给客户端。</li>
 * </ul>
 */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<BaseResponse<?>> handleBusinessException(BusinessException e) {
        HttpStatus status = resolveStatus(e.getStatus());
        if (status == HttpStatus.INTERNAL_SERVER_ERROR) {
            log.error("BusinessException(500): {}", e.getDescription());
        } else {
            log.info("BusinessException({}): {}", e.getStatus(), e.getDescription());
        }
        return ResponseEntity.status(status).body(ResponseUtils.error(e.getDescription(), e.getStatus()));
    }

    /**
     * DTO 字段校验失败：把注解上的具体提示返回给客户端（避免只给笼统的“参数不合法”）。
     */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<BaseResponse<?>> handleValidation(MethodArgumentNotValidException e) {
        String message = e.getBindingResult().getFieldErrors().stream()
                .map(error -> error.getDefaultMessage() == null ? error.getField() + " 不合法"
                        : error.getDefaultMessage())
                .distinct()
                .collect(Collectors.joining("；"));
        if (message.isEmpty()) {
            message = "请求参数不合法";
        }
        log.info("参数校验失败: {}", message);
        return ResponseEntity.badRequest().body(ResponseUtils.error(message, 400));
    }

    @ExceptionHandler({
            MethodArgumentTypeMismatchException.class,
            MissingServletRequestParameterException.class
    })
    public ResponseEntity<BaseResponse<?>> handleBadRequest(Exception e) {
        log.info("参数错误: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResponseUtils.error("请求参数不合法", 400));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<BaseResponse<?>> handleUnreadable(HttpMessageNotReadableException e) {
        log.info("请求体解析失败: {}", e.getMessage());
        return ResponseEntity.badRequest().body(ResponseUtils.error("请求体格式错误", 400));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<BaseResponse<?>> handleMethodNotSupported(HttpRequestMethodNotSupportedException e) {
        return ResponseEntity.status(HttpStatus.METHOD_NOT_ALLOWED)
                .body(ResponseUtils.error("不支持的请求方法", 405));
    }

    /**
     * 路由/静态资源不存在：保持统一响应结构，避免前端拿到 Spring 默认错误页。
     */
    @ExceptionHandler({NoHandlerFoundException.class, NoResourceFoundException.class})
    public ResponseEntity<BaseResponse<?>> handleNotFound(Exception e) {
        log.info("资源不存在: {}", e.getMessage());
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(ResponseUtils.error("资源不存在", 404));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<BaseResponse<?>> handleException(Exception e) {
        // 记录完整堆栈到服务端日志，但不返回给客户端
        log.error("未处理异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(ResponseUtils.error("服务器内部错误", 500));
    }

    private static HttpStatus resolveStatus(int status) {
        HttpStatus resolved = HttpStatus.resolve(status);
        return resolved == null ? HttpStatus.BAD_REQUEST : resolved;
    }
}
