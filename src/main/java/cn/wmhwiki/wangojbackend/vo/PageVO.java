package cn.wmhwiki.wangojbackend.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PageVO<T> {

    @Schema(description = "当前页数")
    Long current;

    @Schema(description = "每页大小")
    Long size;

    @Schema(description = "总数")
    Long total;

    @Schema(description = "泛型数据")
    T t;
}
