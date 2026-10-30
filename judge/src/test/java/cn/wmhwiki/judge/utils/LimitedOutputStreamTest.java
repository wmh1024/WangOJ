/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 有上限输出流的单测（不依赖 Docker）。
 */
class LimitedOutputStreamTest {

    @Test
    void keepsBytesWithinLimit() {
        DockerUtil.LimitedOutputStream stream = new DockerUtil.LimitedOutputStream(10);

        stream.write('a');
        stream.write(new byte[]{'b', 'c'}, 0, 2);

        assertFalse(stream.isExceeded());
        assertEquals("abc", stream.asString());
    }

    @Test
    void truncatesAndMarksWhenLimitExceeded() {
        DockerUtil.LimitedOutputStream stream = new DockerUtil.LimitedOutputStream(4);

        stream.write(new byte[]{'a', 'b', 'c', 'd', 'e', 'f'}, 0, 6);

        assertTrue(stream.isExceeded());
        assertEquals("abcd", stream.asString());
    }

    @Test
    void exactLimitIsNotReportedAsExceeded() {
        DockerUtil.LimitedOutputStream stream = new DockerUtil.LimitedOutputStream(3);

        stream.write(new byte[]{'1', '2', '3'}, 0, 3);
        assertFalse(stream.isExceeded());

        stream.write('4');
        assertTrue(stream.isExceeded());
        assertEquals("123", stream.asString());
    }

    @Test
    void zeroLimitDiscardsEverything() {
        DockerUtil.LimitedOutputStream stream = new DockerUtil.LimitedOutputStream(0);

        stream.write('x');

        assertTrue(stream.isExceeded());
        assertEquals("", stream.asString());
    }
}
