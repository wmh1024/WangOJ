package cn.wmhwiki.wangojbackend.dto;

import cn.wmhwiki.wangojbackend.pojo.LimitOption;
import cn.wmhwiki.wangojbackend.pojo.TestCase;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class ProblemDTO {
    @Schema(description = "题目ID")
    @Positive(message = "题目ID必须为正数")
    private Long id;

    @Schema(description = "题目标题")
    @NotBlank(message = "题目标题不能为空")
    @Size(max = 255, message = "题目标题不能超过 255 个字符")
    private String title;

    @Schema(description = "题目内容")
    @NotBlank(message = "题目内容不能为空")
    private String content;

    @Schema(description = "题目标签")
    @Size(max = 20, message = "题目标签不能超过 20 个")
    private List<@Size(max = 32, message = "单个标签不能超过 32 个字符") String> tags;

    @Schema(description = "测试用例列表")
    @NotEmpty(message = "至少需要一个测试用例")
    @Size(max = 10, message = "测试用例不能超过 10 个")
    @Valid
    private List<TestCase> testCase;

    @Schema(description = "限制选项")
    @Valid
    private LimitOption limitOption;
}
