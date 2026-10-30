package cn.wmhwiki.wangojbackend.interceptor;

import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import com.google.gson.Gson;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.Data;
import org.springframework.web.servlet.HandlerInterceptor;

import java.io.IOException;

/**
 * 管理员拦截器：仅 {@code admin} 角色可访问 {@code /api/admin/**}。
 */
@Data
public class AdminInterceptor implements HandlerInterceptor {

    static final String ADMIN = "admin";

    private static final Gson GSON = new Gson();

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
            throws IOException {
        UserVO userVO = UserHolder.getUserVO();
        if (userVO == null) {
            writeError(response, HttpServletResponse.SC_UNAUTHORIZED, "请先登录");
            return false;
        }
        if (!ADMIN.equals(userVO.getUserRole())) {
            writeError(response, HttpServletResponse.SC_FORBIDDEN, "无权限访问");
            return false;
        }
        return true;
    }

    private static void writeError(HttpServletResponse response, int status, String msg) throws IOException {
        response.setStatus(status);
        response.setContentType("application/json;charset=UTF-8");
        BaseResponse<Void> body = ResponseUtils.error(msg, status);
        response.getWriter().write(GSON.toJson(body));
    }
}
