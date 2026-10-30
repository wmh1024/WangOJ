package cn.wmhwiki.wangojbackend.vo;

import cn.wmhwiki.wangojbackend.pojo.LimitOption;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ProblemVO {
    @Schema(description = "问题ID")
    private Long id;

    @Schema(description = "问题标题")
    private String title;

    @Schema(description = "问题内容")
    private String content;

    @Schema(description = "问题标签列表")
    private List<String> tags;

    @Schema(description = "限制选项")
    private LimitOption limitOption;

    @Schema(description = "提交数量")
    private Integer commitNum;

    @Schema(description = "接受数量")
    private Integer acceptNum;
}