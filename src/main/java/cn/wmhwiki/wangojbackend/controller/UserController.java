package cn.wmhwiki.wangojbackend.controller;

import cn.wmhwiki.wangojbackend.dto.LoginDTO;
import cn.wmhwiki.wangojbackend.dto.UserDTO;
import cn.wmhwiki.wangojbackend.vo.TokenVO;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * @author WangOJ contributors
 */
@RestController
@RequestMapping("/api/user")
@Tag(name = "用户控制器", description = "用于用户操作的控制器")
@Slf4j
public class UserController {
    @Resource
    private UserService userService;

    @Operation(summary = "用户登录")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/login")
    public BaseResponse<TokenVO> userLogin(@Valid @RequestBody LoginDTO loginDTO) {
        return ResponseUtils.success(userService.userLogin(loginDTO));
    }

    @Operation(summary = "获取用户状态")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/status")
    public BaseResponse<UserVO> userStatus() {
        return ResponseUtils.success(userService.userStatus());
    }

    @Operation(summary = "用户注册")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PostMapping("/register")
    public BaseResponse<TokenVO> userRegister(@Valid @RequestBody UserDTO userDTO) {
        return ResponseUtils.success(userService.userRegister(userDTO));
    }

    @Operation(summary = "用户登出")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/logout")
    public BaseResponse<String> userLogout(
            @Parameter(description = "用户认证token", required = true)
            @RequestHeader(value = "Authorization", required = false) String token) {
        return ResponseUtils.success(userService.userLogout(token));
    }

    @Operation(summary = "Ping测试接口")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/ping")
    public BaseResponse<String> ping() {
        return ResponseUtils.success("PONG");
    }
}