package cn.wmhwiki.wangojbackend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserByAdminVO {
    @Schema(description = "用户ID")
    private Long id;

    @Schema(description = "用户账号")
    private String userAccount;

    @Schema(description = "用户角色")
    private String userRole;

    @Schema(description = "创建时间")
    private String createdTime;

    @Schema(description = "最后登录时间")
    private String lastLoginTime;
}
