package cn.wmhwiki.wangojbackend.common;

/**
 * 返回响应工具类
 *
 * @author WangOJ contributors
 */
public class ResponseUtils {
    public static <T> BaseResponse<T> success(T data) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(200);
        response.setData(data);
        response.setMsg("success");
        return response;
    }

    public static <T> BaseResponse<T> error(String msg) {
        return error(msg, 400);
    }

    /**
     * 错误响应，{@code code} 与 HTTP 状态码保持一致，便于前端区分 401/403/429。
     */
    public static <T> BaseResponse<T> error(String msg, int code) {
        BaseResponse<T> response = new BaseResponse<>();
        response.setCode(code);
        response.setMsg(msg);
        return response;
    }
}