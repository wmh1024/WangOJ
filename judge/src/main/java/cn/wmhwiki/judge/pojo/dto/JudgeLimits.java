/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 协议 v1.0 中的资源限制。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeLimits {

    // 单个测试点的运行时间限制（毫秒）
    private Long timeLimitMs;

    // 内存限制（MB）
    private Long memoryLimitMb;

    // 编译时间限制（毫秒）
    private Long compileTimeLimitMs;

    // 单个测试点最多返回的输出字节数
    private Long maxOutputBytes;
}
