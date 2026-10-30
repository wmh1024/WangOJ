package cn.wmhwiki.wangojbackend.interceptor;

import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import com.google.gson.Gson;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 登录拦截器：未登录时返回 401 与统一响应体。
 *
 * <p>是否登录由 {@code TokenInterceptor} 回源数据库后写入 {@link UserHolder}，
 * 因此"账号被删除/被禁用"也会立即失去登录态。</p>
 */
public class LoginInterceptor implements HandlerInterceptor {

    private static final Gson GSON = new Gson();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        if (UserHolder.getUserVO() != null) {
            return true;
        }
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json;charset=UTF-8");
        BaseResponse<Void> body = ResponseUtils.error("请先登录", 401);
        response.getWriter().write(GSON.toJson(body));
        return false;
    }
}
