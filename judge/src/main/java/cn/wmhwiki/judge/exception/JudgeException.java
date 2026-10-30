/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.exception;

import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import lombok.Data;
import lombok.EqualsAndHashCode;

/**
 * 判题协议异常。
 *
 * <p>携带 HTTP 状态码与协议响应，便于全局异常处理器直接返回协议格式。</p>
 */
@Data
@EqualsAndHashCode(callSuper = false)
public class JudgeException extends RuntimeException {

    private final int httpStatus;

    private final String requestId;

    public JudgeException(int httpStatus, String requestId, String message) {
        super(message);
        this.httpStatus = httpStatus;
        this.requestId = requestId;
    }

    public JudgeResponse toResponse() {
        return JudgeResponse.error(requestId, httpStatus, getMessage());
    }
}
