/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.utils;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 退出码映射单测（不访问 Docker）。
 */
class DockerUtilExitCodeTest {

    @Test
    void zeroIsAccepted() {
        assertEquals("ACCEPTED", DockerUtil.mapExitCode(0, false));
    }

    @Test
    void signalExitCodesAreMemoryLimit() {
        assertEquals("MEMORY_LIMIT_EXCEEDED", DockerUtil.mapExitCode(137, false));
        assertEquals("MEMORY_LIMIT_EXCEEDED", DockerUtil.mapExitCode(134, false));
        assertEquals("MEMORY_LIMIT_EXCEEDED", DockerUtil.mapExitCode(143, false));
    }

    @Test
    void jvmOomExitCodeIsMemoryLimit() {
        assertEquals("MEMORY_LIMIT_EXCEEDED", DockerUtil.mapExitCode(3, true));
        assertEquals("MEMORY_LIMIT_EXCEEDED", DockerUtil.mapExitCode(1, true));
    }

    @Test
    void sameExitCodeWithoutJvmOomIsRuntimeError() {
        assertEquals("RUNTIME_ERROR", DockerUtil.mapExitCode(3, false));
        assertEquals("RUNTIME_ERROR", DockerUtil.mapExitCode(1, false));
    }
}
