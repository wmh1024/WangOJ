/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 单个测试点的执行结果。
 *
 * <p>注意：这里的 {@code verdict} 只描述“执行层面”的结论
 * （ACCEPTED / TIME_LIMIT_EXCEEDED / MEMORY_LIMIT_EXCEEDED / RUNTIME_ERROR / SYSTEM_ERROR），
 * 答案比较（WRONG_ANSWER）由后端完成。</p>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeCaseResult {

    // 测试点下标，从 0 开始
    private Integer index;

    // 执行层面结论
    private String verdict;

    // 标准输出（已截断）
    private String stdout;

    // 标准错误（已截断）
    private String stderr;

    // 耗时（毫秒）
    private Long timeMs;

    // 内存占用（字节）
    private Long memoryBytes;
}
