package cn.wmhwiki.wangojbackend.pojo;

import cn.wmhwiki.wangojbackend.constant.JudgeLimits;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class LimitOption {
    @Schema(description = "超时限制")
    @Min(value = JudgeLimits.MIN_TIME_LIMIT_MS, message = "时间限制不能小于 200ms")
    @Max(value = JudgeLimits.MAX_TIME_LIMIT_MS, message = "时间限制不能超过 5000ms")
    private Long timeoutLimit;

    @Schema(description = "内存限制")
    @Min(value = JudgeLimits.MIN_MEMORY_LIMIT_MB, message = "内存限制不能小于 32MB")
    @Max(value = JudgeLimits.MAX_MEMORY_LIMIT_MB, message = "内存限制不能超过 512MB")
    private Long memoryLimit;
}
