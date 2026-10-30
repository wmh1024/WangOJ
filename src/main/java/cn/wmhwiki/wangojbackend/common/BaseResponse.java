package cn.wmhwiki.wangojbackend.common;

import lombok.Data;

import java.io.Serializable;

/**
 * 通用响应结果
 *
 * @author WangOJ contributors
 */
@Data
public class BaseResponse<T> implements Serializable {
    private int code;
    private T data;
    private String msg;
}