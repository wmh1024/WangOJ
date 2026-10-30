package cn.wmhwiki.wangojbackend.constant;

/**
 * 判题状态与执行结果常量。
 *
 * <p>数据库 {@code commitLog.status} 与前端展示共用这里的值。</p>
 *
 * @author WangOJ contributors
 */
public final class JudgeStatus {

    private JudgeStatus() {
    }

    /**
     * 已提交，等待判题
     */
    public static final String WAITING = "Waiting";

    /**
     * 通过
     */
    public static final String ACCEPT = "Accept";

    /**
     * 答案错误
     */
    public static final String WRONG_ANSWER = "Wrong Answer";

    /**
     * 编译错误
     */
    public static final String COMPILE_ERROR = "Compilation Error";

    /**
     * 运行超时
     */
    public static final String TIME_LIMIT_EXCEEDED = "Time Limit Exceeded";

    /**
     * 内存超限
     */
    public static final String MEMORY_LIMIT_EXCEEDED = "Memory Limit Exceeded";

    /**
     * 运行时错误
     */
    public static final String RUNTIME_ERROR = "Runtime Error";

    /**
     * 系统错误（沙箱不可用、内部异常等）
     */
    public static final String SYSTEM_ERROR = "System Error";

    // ---------- 沙箱单测试点 verdict ----------

    public static final String CASE_ACCEPTED = "ACCEPTED";
    public static final String CASE_WRONG_ANSWER = "WRONG_ANSWER";
    public static final String CASE_TIME_LIMIT_EXCEEDED = "TIME_LIMIT_EXCEEDED";
    public static final String CASE_MEMORY_LIMIT_EXCEEDED = "MEMORY_LIMIT_EXCEEDED";
    public static final String CASE_RUNTIME_ERROR = "RUNTIME_ERROR";
    public static final String CASE_SYSTEM_ERROR = "SYSTEM_ERROR";
    public static final String CASE_COMPILE_ERROR = "COMPILE_ERROR";
}
