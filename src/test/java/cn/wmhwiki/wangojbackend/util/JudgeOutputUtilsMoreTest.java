package cn.wmhwiki.wangojbackend.util;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 输出比较与判决汇总的单元测试（纯函数，无外部依赖）。
 */
class JudgeOutputUtilsMoreTest {

    @Test
    void crlfAndLfAreEquivalent() {
        assertTrue(JudgeOutputUtils.isAccepted("1\r\n2\r\n", "1\n2\n"));
        assertTrue(JudgeOutputUtils.isAccepted("1\r2\r", "1\n2"));
    }

    @Test
    void trailingSpacesOnLinesAreIgnored() {
        assertTrue(JudgeOutputUtils.isAccepted("abc\n", "abc   \n"));
        assertTrue(JudgeOutputUtils.isAccepted("a b\n", "a b\t\n"));
    }

    @Test
    void leadingSpacesAreSignificant() {
        assertFalse(JudgeOutputUtils.isAccepted("abc", " abc"));
    }

    @Test
    void extraBlankLinesAtEndAreIgnored() {
        assertTrue(JudgeOutputUtils.isAccepted("1\n2\n", "1\n2\n\n\n"));
        assertTrue(JudgeOutputUtils.isAccepted("1\n2", "1\n2"));
    }

    @Test
    void emptyOutputOnlyMatchesEmptyExpected() {
        assertTrue(JudgeOutputUtils.isAccepted("", ""));
        assertTrue(JudgeOutputUtils.isAccepted("\n\n", ""));
        assertFalse(JudgeOutputUtils.isAccepted("1", ""));
        assertFalse(JudgeOutputUtils.isAccepted("", "1"));
    }

    @Test
    void orderAndMultiplicityMatter() {
        assertFalse(JudgeOutputUtils.isAccepted("1\n2", "2\n1"));
        assertFalse(JudgeOutputUtils.isAccepted("1\n2", "1\n2\n2"));
        assertFalse(JudgeOutputUtils.isAccepted("1\n2", "1"));
    }

    @Test
    void nullTreatedAsEmpty() {
        assertEquals("", JudgeOutputUtils.normalize(null));
        assertTrue(JudgeOutputUtils.isAccepted(null, null));
        assertTrue(JudgeOutputUtils.isAccepted(null, "  \n"));
    }
}
