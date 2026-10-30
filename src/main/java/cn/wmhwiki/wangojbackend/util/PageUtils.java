package cn.wmhwiki.wangojbackend.util;

/**
 * 分页参数归一化。
 *
 * <p>避免前端传入 0、负数或超大页大小导致的异常与慢查询。</p>
 */
public final class PageUtils {

    public static final int DEFAULT_PAGE = 1;

    public static final int DEFAULT_PAGE_SIZE = 10;

    public static final int MAX_PAGE_SIZE = 100;

    private PageUtils() {
    }

    /**
     * 归一化页码：小于 1 一律按第 1 页处理。
     */
    public static int page(Object current) {
        Long value = toLong(current);
        if (value == null || value < 1) {
            return DEFAULT_PAGE;
        }
        // 防止极大页码导致 OFFSET 溢出
        return (int) Math.min(value, 1_000_000L);
    }

    /**
     * 归一化页大小：范围 1~100，缺省 10。
     */
    public static int pageSize(Object pageSize) {
        Long value = toLong(pageSize);
        if (value == null || value < 1) {
            return DEFAULT_PAGE_SIZE;
        }
        return (int) Math.min(value, MAX_PAGE_SIZE);
    }

    private static Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(value.toString().trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
