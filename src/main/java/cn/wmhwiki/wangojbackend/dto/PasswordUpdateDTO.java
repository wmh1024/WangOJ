package cn.wmhwiki.wangojbackend.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

/**
 * 管理员重置用户密码的请求体。
 *
 * <p>只包含密码字段：账号来自路径参数，不能要求调用方重复提交未被使用的 userAccount。
 * 兼容旧客户端仍携带 userAccount 的请求体（忽略未知字段）。</p>
 */
@Data
@JsonIgnoreProperties(ignoreUnknown = true)
public class PasswordUpdateDTO {
    @Schema(description = "新密码")
    @NotBlank(message = "密码不能为空")
    @Size(max = 64, message = "密码过长")
    private String userPassword;
}
