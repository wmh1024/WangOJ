/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.lang;

import cn.wmhwiki.judge.constant.enums.LangContextEnum;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 语言命令构造单测（不访问 Docker）。
 */
class LangContextCommandTest {

    private static final String SOURCE = "/data/main.c";

    @Test
    void javaRunCmdUsesHeapFractionAndOomExit() {
        JavaContext context = new JavaContext();
        String[] cmd = context.buildRunCmd(512);

        assertTrue(Arrays.asList(cmd).contains("-Xmx384m"));
        assertTrue(Arrays.asList(cmd).contains("-XX:+UseSerialGC"));
        assertTrue(Arrays.asList(cmd).contains("-XX:+ExitOnOutOfMemoryError"));
        assertTrue(Arrays.asList(cmd).contains("Main"));
    }

    @Test
    void javaRunCmdNeverGoesBelowMinimumHeap() {
        JavaContext context = new JavaContext();

        assertTrue(Arrays.asList(context.buildRunCmd(0)).contains("-Xmx16m"));
        assertTrue(Arrays.asList(context.buildRunCmd(32)).contains("-Xmx24m"));
    }

    @Test
    void buildRunCmdReturnsFreshArray() {
        LangContext context = new CContext();
        String[] first = context.buildRunCmd(256);
        first[0] = "tampered";

        assertArrayEquals(new String[]{"./main"}, context.buildRunCmd(256));
        assertArrayEquals(new String[]{"./main"}, context.getRunCmd());
    }

    @Test
    void allLanguagesProduceNonEmptyRunCmd() {
        for (LangContextEnum item : LangContextEnum.values()) {
            String[] cmd = item.getLangContext().buildRunCmd(256);
            assertTrue(cmd != null && cmd.length > 0, item.name() + " 缺少启动命令");
            for (String arg : cmd) {
                assertFalse(arg == null || arg.isEmpty(), item.name() + " 存在空参数");
            }
        }
    }

    @Test
    void cCompileCmdIsStructuredAndExpanded() {
        String[] cmd = new CContext().buildCompileCmd(SOURCE);

        assertEquals("gcc", cmd[0]);
        assertTrue(Arrays.asList(cmd).contains(SOURCE));
        assertFalse(Arrays.asList(cmd).contains(LangContext.SOURCE_PLACEHOLDER));
        for (String arg : cmd) {
            assertFalse(arg.isEmpty(), "编译命令不能包含空字符串参数");
        }
    }

    @Test
    void javaCompileCmdUsesUtf8() {
        String[] cmd = new JavaContext().buildCompileCmd("/data/Main.java");

        assertArrayEquals(new String[]{"javac", "-encoding", "utf-8", "/data/Main.java"}, cmd);
    }

    @Test
    void interpretedLanguagesNeedNoCompile() {
        assertNull(new PythonContext().buildCompileCmd("/data/main.py"));
        assertNull(new JsContext().buildCompileCmd("/data/main.js"));
    }

    @Test
    void compileTemplateIsExpandedIntoFreshArray() {
        CContext context = new CContext();
        String[] first = context.buildCompileCmd(SOURCE);
        first[0] = "tampered";

        assertEquals("gcc", context.buildCompileCmd(SOURCE)[0]);
        assertEquals(LangContext.SOURCE_PLACEHOLDER, context.getCompileCmd()[3]);
    }

    @Test
    void onlyJavaTreatsOomExitAsMemoryLimit() {
        JavaContext java = new JavaContext();

        assertTrue(java.isJvmOutOfMemoryExit(3, ""));
        assertTrue(java.isJvmOutOfMemoryExit(1,
                "Terminating due to java.lang.OutOfMemoryError: Java heap space"));
        assertFalse(java.isJvmOutOfMemoryExit(0,
                "Terminating due to java.lang.OutOfMemoryError"));
        assertFalse(new CContext().isJvmOutOfMemoryExit(3, "OutOfMemoryError"));
    }
}
