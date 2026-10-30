/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 判题协议 v1.0 响应体（JSON）。
 *
 * <pre>{@code
 * {
 *   "requestId": "commit-1234-uuid",
 *   "status": "success",
 *   "code": 200,
 *   "message": "judged",
 *   "verdict": "EXECUTED",
 *   "compileOutput": "",
 *   "cases": [ { "index": 0, "verdict": "ACCEPTED", "stdout": "3\n",
 *                "stderr": "", "timeMs": 12, "memoryBytes": 2048 } ],
 *   "totalTimeMs": 22,
 *   "maxMemoryBytes": 2048
 * }
 * }</pre>
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgeResponse {

    public static final String STATUS_SUCCESS = "success";
    public static final String STATUS_ERROR = "error";

    /**
     * 整体 verdict：沙箱只负责执行阶段，答案判定（WRONG_ANSWER）由后端完成。
     * 取值：EXECUTED / COMPILE_ERROR / SYSTEM_ERROR
     */
    public static final String VERDICT_EXECUTED = "EXECUTED";
    public static final String VERDICT_COMPILE_ERROR = "COMPILE_ERROR";
    public static final String VERDICT_SYSTEM_ERROR = "SYSTEM_ERROR";

    // 与请求一致的请求 ID
    private String requestId;

    // success / error
    private String status;

    // 200 成功；400 参数错误；401 未认证；413 超限；500 内部错误
    private Integer code;

    // 人类可读信息
    private String message;

    // EXECUTED / COMPILE_ERROR / SYSTEM_ERROR
    private String verdict;

    // 编译输出（截断）
    private String compileOutput;

    // 每个测试点的执行结果
    private List<JudgeCaseResult> cases;

    // 所有测试点耗时合计（毫秒）
    private Long totalTimeMs;

    // 所有测试点内存峰值（字节）
    private Long maxMemoryBytes;

    public static JudgeResponse error(String requestId, Integer code, String message) {
        JudgeResponse response = new JudgeResponse();
        response.setRequestId(requestId);
        response.setStatus(STATUS_ERROR);
        response.setCode(code);
        response.setMessage(message);
        response.setVerdict(VERDICT_SYSTEM_ERROR);
        response.setCompileOutput("");
        return response;
    }

    public static JudgeResponse success(String requestId) {
        JudgeResponse response = new JudgeResponse();
        response.setRequestId(requestId);
        response.setStatus(STATUS_SUCCESS);
        response.setCode(200);
        response.setMessage("judged");
        response.setVerdict(VERDICT_EXECUTED);
        response.setCompileOutput("");
        return response;
    }
}
