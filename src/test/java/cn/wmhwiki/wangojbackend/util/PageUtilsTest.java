package cn.wmhwiki.wangojbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 分页参数归一化单元测试（纯函数，无外部依赖）。
 */
class PageUtilsTest {

    @Test
    void pageFallsBackToFirstPage() {
        assertEquals(1, PageUtils.page(null));
        assertEquals(1, PageUtils.page(0));
        assertEquals(1, PageUtils.page(-5));
        assertEquals(3, PageUtils.page(3));
        assertEquals(3, PageUtils.page(3L));
        assertEquals(1, PageUtils.page("abc"));
    }

    @Test
    void pageIsCapped() {
        assertEquals(1_000_000, PageUtils.page(9_999_999L));
    }

    @Test
    void pageSizeIsBounded() {
        assertEquals(10, PageUtils.pageSize(null));
        assertEquals(10, PageUtils.pageSize(0));
        assertEquals(1, PageUtils.pageSize(1));
        assertEquals(100, PageUtils.pageSize(1000));
        assertEquals(100, PageUtils.pageSize(101));
        assertEquals(20, PageUtils.pageSize("20"));
    }

    @Test
    void pageSizeRejectsGarbage() {
        assertEquals(10, PageUtils.pageSize("   "));
        assertEquals(10, PageUtils.pageSize("x"));
    }
}
