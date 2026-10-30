/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 判题协议 v1.0 请求体（JSON）。
 *
 * <p>示例：</p>
 * <pre>{@code
 * {
 *   "requestId": "commit-1234-uuid",
 *   "protocolVersion": "1.0",
 *   "language": "cpp",
 *   "code": "int main() { ... }",
 *   "mode": "judge",
 *   "limits": { "timeLimitMs": 1000, "memoryLimitMb": 256,
 *               "compileTimeLimitMs": 5000, "maxOutputBytes": 4096 },
 *   "testCases": [ { "input": "1 2\n", "expectedOutput": "3\n" } ]
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeRequest {

    // 请求 ID，由后端生成并贯穿日志
    private String requestId;

    // 协议版本，仅支持 1.0
    private String protocolVersion;

    // 语言：c / c++ / java / python / javascript / golang
    private String language;

    // 用户代码
    private String code;

    // 模式：judge（判题） / run（仅执行）
    private String mode;

    // 资源限制
    private JudgeLimits limits;

    // 测试点列表
    private List<JudgeTestCase> testCases;
}
