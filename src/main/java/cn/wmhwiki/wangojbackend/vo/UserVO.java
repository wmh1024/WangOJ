package cn.wmhwiki.wangojbackend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

@Data
public class UserVO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户账号")
    private String userAccount;

    @Schema(description = "用户角色")
    private String userRole;
}