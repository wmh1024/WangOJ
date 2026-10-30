package cn.wmhwiki.wangojbackend.constant;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JudgeLimitsTest {

    @Test
    void nullAndNonPositiveFallBackToDefaults() {
        assertEquals(JudgeLimits.DEFAULT_TIME_LIMIT_MS, JudgeLimits.normalizeTimeLimit(null));
        assertEquals(JudgeLimits.DEFAULT_TIME_LIMIT_MS, JudgeLimits.normalizeTimeLimit(0L));
        assertEquals(JudgeLimits.DEFAULT_MEMORY_LIMIT_MB, JudgeLimits.normalizeMemoryLimit(null));
        assertEquals(JudgeLimits.DEFAULT_MEMORY_LIMIT_MB, JudgeLimits.normalizeMemoryLimit(-1L));
    }

    @Test
    void valuesOutsideRangeAreClamped() {
        assertEquals(JudgeLimits.MIN_TIME_LIMIT_MS, JudgeLimits.normalizeTimeLimit(1L));
        assertEquals(JudgeLimits.MAX_TIME_LIMIT_MS, JudgeLimits.normalizeTimeLimit(999_999L));
        assertEquals(JudgeLimits.MIN_MEMORY_LIMIT_MB, JudgeLimits.normalizeMemoryLimit(1L));
        assertEquals(JudgeLimits.MAX_MEMORY_LIMIT_MB, JudgeLimits.normalizeMemoryLimit(999_999L));
    }

    @Test
    void valuesInsideRangeAreKept() {
        assertEquals(1000L, JudgeLimits.normalizeTimeLimit(1000L));
        assertEquals(256L, JudgeLimits.normalizeMemoryLimit(256L));
        assertEquals(200L, JudgeLimits.normalizeTimeLimit(200L));
        assertEquals(5000L, JudgeLimits.normalizeTimeLimit(5000L));
        assertEquals(32L, JudgeLimits.normalizeMemoryLimit(32L));
        assertEquals(512L, JudgeLimits.normalizeMemoryLimit(512L));
    }
}
