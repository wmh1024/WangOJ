package cn.wmhwiki.wangojbackend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class CommitlogVO {

    @Schema(description = "超时限制")
    Long timeoutLimit;

    @Schema(description = "内存限制")
    Long memoryLimit;

    @Schema(description = "ID")
    private Long id;

    @Schema(description = "用户ID")
    private Long userId;

    @Schema(description = "问题ID")
    private Long problemId;

    @Schema(description = "问题标题")
    private String problemTitle;

    @Schema(description = "题目标签")
    private List<String> problemTags;

    @Schema(description = "代码")
    private String code;

    @Schema(description = "状态")
    private String status;

    @Schema(description = "语言")
    private String lang;

    @Schema(description = "创建时间")
    private String createdTime;

    @Schema(description = "编译错误 / 系统错误信息（仅详情返回）")
    private String errorMessage;

    @Schema(description = "逐测试点结果 JSON：[{index,verdict,timeMs,memoryBytes,accepted}]（仅详情返回）")
    private String caseResult;
}