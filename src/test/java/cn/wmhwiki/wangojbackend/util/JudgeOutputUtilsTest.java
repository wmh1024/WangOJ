package cn.wmhwiki.wangojbackend.util;

import cn.wmhwiki.wangojbackend.constant.JudgeStatus;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class JudgeOutputUtilsTest {

    @Test
    void normalizeHandlesLineEndingsAndTrailingWhitespace() {
        assertEquals("1\n2", JudgeOutputUtils.normalize("1  \r\n2\t\r\n\r\n"));
        assertEquals("", JudgeOutputUtils.normalize(null));
        assertEquals("", JudgeOutputUtils.normalize("\n\n\n"));
    }

    @Test
    void isAcceptedIgnoresOnlyHarmlessDifferences() {
        assertTrue(JudgeOutputUtils.isAccepted("3\n", "3   \n\n"));
        assertTrue(JudgeOutputUtils.isAccepted("a\r\nb\r\n", "a\nb"));
        // 顺序不同不算通过
        assertFalse(JudgeOutputUtils.isAccepted("1\n2", "2\n1"));
        // 重复次数不同不算通过
        assertFalse(JudgeOutputUtils.isAccepted("1\n2", "1\n2\n2"));
        // 少一行不算通过
        assertFalse(JudgeOutputUtils.isAccepted("1\n2", "1"));
        // 大小写默认敏感
        assertFalse(JudgeOutputUtils.isAccepted("YES", "yes"));
    }

    @Test
    void resolveCaseVerdictRespectsSandboxAndLimits() {
        assertEquals(JudgeStatus.CASE_ACCEPTED,
                JudgeOutputUtils.resolveCaseVerdict("ACCEPTED", "3\n", "3", 10L, 100L, 1000L, 262144L));
        // 沙箱声称成功但缺少内存/耗时：不能“未知即通过”
        assertEquals(JudgeStatus.CASE_SYSTEM_ERROR,
                JudgeOutputUtils.resolveCaseVerdict("ACCEPTED", "3\n", "3", 10L, null, 1000L, 262144L));
        assertEquals(JudgeStatus.CASE_SYSTEM_ERROR,
                JudgeOutputUtils.resolveCaseVerdict("ACCEPTED", "3\n", "3", null, 100L, 1000L, 262144L));
        assertEquals(JudgeStatus.CASE_WRONG_ANSWER,
                JudgeOutputUtils.resolveCaseVerdict("ACCEPTED", "3\n", "4", 10L, 100L, 1000L, 262144L));
        // 沙箱超时优先级高于输出比较
        assertEquals(JudgeStatus.CASE_TIME_LIMIT_EXCEEDED,
                JudgeOutputUtils.resolveCaseVerdict("TIME_LIMIT_EXCEEDED", "3\n", "3", 1001L, 100L, 1000L, 262144L));
        // 沙箱没给出 verdict 时，后端按耗时兜底判定 TLE
        assertEquals(JudgeStatus.CASE_TIME_LIMIT_EXCEEDED,
                JudgeOutputUtils.resolveCaseVerdict(null, "3\n", "3", 1001L, 100L, 1000L, 262144L));
        assertEquals(JudgeStatus.CASE_MEMORY_LIMIT_EXCEEDED,
                JudgeOutputUtils.resolveCaseVerdict(null, "3\n", "3", 10L, 300000L, 1000L, 262144L));
        assertEquals(JudgeStatus.CASE_RUNTIME_ERROR,
                JudgeOutputUtils.resolveCaseVerdict("RUNTIME_ERROR", "3\n", "", 10L, 100L, 1000L, 262144L));
    }

    @Test
    void aggregateVerdictUsesStablePriority() {
        assertEquals(JudgeStatus.ACCEPT,
                JudgeOutputUtils.aggregateVerdict(Arrays.asList("ACCEPTED", "ACCEPTED")));
        assertEquals(JudgeStatus.WRONG_ANSWER,
                JudgeOutputUtils.aggregateVerdict(Arrays.asList("ACCEPTED", "WRONG_ANSWER")));
        assertEquals(JudgeStatus.TIME_LIMIT_EXCEEDED,
                JudgeOutputUtils.aggregateVerdict(Arrays.asList("WRONG_ANSWER", "TIME_LIMIT_EXCEEDED")));
        assertEquals(JudgeStatus.COMPILE_ERROR,
                JudgeOutputUtils.aggregateVerdict(Arrays.asList("TIME_LIMIT_EXCEEDED", "COMPILE_ERROR")));
        assertEquals(JudgeStatus.SYSTEM_ERROR,
                JudgeOutputUtils.aggregateVerdict(Arrays.asList("WRONG_ANSWER", "SYSTEM_ERROR")));
        assertEquals(JudgeStatus.SYSTEM_ERROR, JudgeOutputUtils.aggregateVerdict(Collections.emptyList()));
        // 未知/全空 verdict 不能被默认当成 AC
        assertEquals(JudgeStatus.SYSTEM_ERROR,
                JudgeOutputUtils.aggregateVerdict(Collections.singletonList((String) null)));
        assertEquals(JudgeStatus.SYSTEM_ERROR,
                JudgeOutputUtils.aggregateVerdict(Collections.singletonList("FUTURE_STATUS")));
        assertEquals(JudgeStatus.ACCEPT,
                JudgeOutputUtils.aggregateVerdict(Arrays.asList("ACCEPTED", null)));
    }
}
