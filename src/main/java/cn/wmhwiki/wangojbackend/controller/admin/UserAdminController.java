package cn.wmhwiki.wangojbackend.controller.admin;

import cn.wmhwiki.wangojbackend.dto.PasswordUpdateDTO;
import cn.wmhwiki.wangojbackend.vo.PageVO;
import cn.wmhwiki.wangojbackend.vo.UserByAdminVO;
import cn.wmhwiki.wangojbackend.common.BaseResponse;
import cn.wmhwiki.wangojbackend.common.ResponseUtils;
import cn.wmhwiki.wangojbackend.domain.User;
import cn.wmhwiki.wangojbackend.service.UserService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin/user")
@Tag(name = "用户管理控制器", description = "用于管理用户的控制器")
public class UserAdminController {
    @Resource
    private UserService userService;

    @Operation(summary = "获取用户详情")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("/{id}")
    public BaseResponse<User> getUserById(@PathVariable("id") Long id) {
        return ResponseUtils.success(userService.getUserById(id));
    }

    @Operation(summary = "获取用户列表")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @GetMapping("")
    public BaseResponse<PageVO<List<UserByAdminVO>>> getUserList(Long current, Long pageSize) {
        return ResponseUtils.success(userService.getUserList(current, pageSize));
    }

    @Operation(summary = "修改用户密码")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @PutMapping("/{id}")
    public BaseResponse<String> updateUser(@PathVariable Long id,
                                           @Valid @RequestBody PasswordUpdateDTO passwordUpdateDTO) {
        return ResponseUtils.success(userService.updateUser(id, passwordUpdateDTO));
    }

    @Operation(summary = "删除用户")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "成功"),
            @ApiResponse(responseCode = "500", description = "服务器内部错误")
    })
    @DeleteMapping("/{id}")
    public BaseResponse<String> delUserById(@PathVariable Long id) {
        return ResponseUtils.success(userService.delUserById(id));
    }

}