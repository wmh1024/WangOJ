package cn.wmhwiki.wangojbackend.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitlogDTO {
    @Schema(description = "问题ID")
    @NotNull(message = "题目ID不能为空")
    @Positive(message = "题目ID必须为正数")
    private Long problemId;

    @Schema(description = "语言")
    @NotBlank(message = "语言不能为空")
    @Size(max = 32, message = "语言标识过长")
    private String lang;

    @Schema(description = "提交的代码")
    @NotBlank(message = "代码不能为空")
    @Size(max = 64 * 1024, message = "代码长度不能超过 65536 个字符")
    private String code;
}
