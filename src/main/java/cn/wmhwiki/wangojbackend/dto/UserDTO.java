package cn.wmhwiki.wangojbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class UserDTO {
    @Schema(description = "用户账号")
    @NotBlank(message = "用户名不能为空")
    @Size(max = 64, message = "用户名过长")
    private String userAccount;

    @Schema(description = "用户密码")
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码过长")
    private String userPassword;
}
