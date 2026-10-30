package cn.wmhwiki.wangojbackend.pojo;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class TestCase {
    @Schema(description = "测试用例输入")
    @Size(max = 64 * 1024, message = "单个测试输入不能超过 65536 个字符")
    private String input;

    @Schema(description = "测试用例输出")
    @Size(max = 64 * 1024, message = "单个测试输出不能超过 65536 个字符")
    private String output;
}
