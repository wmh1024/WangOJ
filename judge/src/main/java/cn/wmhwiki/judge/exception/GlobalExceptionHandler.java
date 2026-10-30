/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.exception;

import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 统一异常处理：所有错误都返回协议 v1.0 的 JSON 结构，并给出正确的 HTTP 状态码。
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(JudgeException.class)
    public ResponseEntity<JudgeResponse> handleJudgeException(JudgeException ex) {
        log.warn("判题请求错误, requestId={}, status={}, message={}",
                ex.getRequestId(), ex.getHttpStatus(), ex.getMessage());
        return ResponseEntity.status(ex.getHttpStatus()).body(ex.toResponse());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<JudgeResponse> handleBadBody(HttpMessageNotReadableException ex) {
        log.warn("请求体解析失败: {}", ex.getMessage());
        return ResponseEntity.badRequest()
                .body(JudgeResponse.error(null, 400, "请求体不是合法的 JSON 或字段类型不匹配"));
    }

    @ExceptionHandler(HttpMediaTypeNotSupportedException.class)
    public ResponseEntity<JudgeResponse> handleMediaType(HttpMediaTypeNotSupportedException ex) {
        return ResponseEntity.status(HttpStatus.UNSUPPORTED_MEDIA_TYPE)
                .body(JudgeResponse.error(null, 415, "请使用 application/json 提交"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<JudgeResponse> handleException(Exception ex) {
        log.error("判题服务内部错误", ex);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(JudgeResponse.error(null, 500, "判题服务内部错误"));
    }
}
