package cn.wmhwiki.wangojbackend.constant;

/**
 * 题目时间/内存限制的统一边界。
 *
 * <p>后端保存、展示与沙箱执行必须使用同一套边界，避免出现“题目页写 999999ms、
 * 实际判题却按 5000ms 执行”的不一致。</p>
 */
public final class JudgeLimits {

    public static final long DEFAULT_TIME_LIMIT_MS = 1000L;

    public static final long MIN_TIME_LIMIT_MS = 200L;

    public static final long MAX_TIME_LIMIT_MS = 5000L;

    public static final long DEFAULT_MEMORY_LIMIT_MB = 256L;

    public static final long MIN_MEMORY_LIMIT_MB = 32L;

    public static final long MAX_MEMORY_LIMIT_MB = 512L;

    /**
     * 单次请求最多测试点数，与沙箱 Constants.maxTestCases 保持一致。
     * 契约由仓库根目录 judge-limits.json 与两端契约测试保证。
     */
    public static final int MAX_TEST_CASES = 10;

    private JudgeLimits() {
    }

    /**
     * 空值/非正数回落默认值，其余值收敛到 [200, 5000] 毫秒。
     */
    public static long normalizeTimeLimit(Long value) {
        if (value == null || value <= 0) {
            return DEFAULT_TIME_LIMIT_MS;
        }
        return Math.max(MIN_TIME_LIMIT_MS, Math.min(value, MAX_TIME_LIMIT_MS));
    }

    /**
     * 空值/非正数回落默认值，其余值收敛到 [32, 512] MB。
     */
    public static long normalizeMemoryLimit(Long value) {
        if (value == null || value <= 0) {
            return DEFAULT_MEMORY_LIMIT_MB;
        }
        return Math.max(MIN_MEMORY_LIMIT_MB, Math.min(value, MAX_MEMORY_LIMIT_MB));
    }
}
