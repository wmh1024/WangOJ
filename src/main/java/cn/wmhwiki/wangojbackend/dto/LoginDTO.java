package cn.wmhwiki.wangojbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 登录请求体。
 *
 * <p>校验提示统一为“用户名或密码错误”，避免通过字段级提示区分账号是否存在；
 * 注册/改密仍使用 {@link UserDTO} 的具体提示。</p>
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class LoginDTO {
    @Schema(description = "用户账号")
    @NotBlank(message = "用户名或密码错误")
    @Size(max = 64, message = "用户名或密码错误")
    private String userAccount;

    @Schema(description = "用户密码")
    @NotBlank(message = "用户名或密码错误")
    @Size(max = 64, message = "用户名或密码错误")
    private String userPassword;
}
