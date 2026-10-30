package cn.wmhwiki.wangojbackend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;


@Data
@AllArgsConstructor
public class TokenVO {
    @Schema(description = "访问令牌")
    private String token;

    @Schema(description = "用户信息")
    private UserVO userVO;
}