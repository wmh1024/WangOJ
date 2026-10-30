/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.utils;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.File;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 判题工作目录文件操作单测（不依赖 Docker）。
 */
class FileUtilTest {

    @TempDir
    Path tempDir;

    @Test
    void savesUtf8ContentInNestedDirectory() throws Exception {
        String folder = tempDir.resolve("case-1").toString();

        File saved = FileUtil.saveFile(folder, "main.c", "// 中文注释\nint main() { return 0; }");

        assertTrue(saved.isDirectory());
        File file = new File(saved, "main.c");
        assertTrue(file.isFile());
        assertEquals("// 中文注释\nint main() { return 0; }",
                new String(Files.readAllBytes(file.toPath()), StandardCharsets.UTF_8));
    }

    @Test
    void nullContentBecomesEmptyFile() {
        File folder = FileUtil.saveFile(tempDir.resolve("case-empty").toString(), "in.txt", null);

        File file = new File(folder, "in.txt");
        assertTrue(file.isFile());
        assertEquals(0L, file.length());
    }

    @Test
    void deleteIsIdempotent() throws Exception {
        File folder = FileUtil.saveFile(tempDir.resolve("case-delete").toString(), "a.txt", "x");
        File child = new File(folder, "nested");
        assertTrue(child.mkdir());
        Files.write(new File(child, "b.txt").toPath(), new byte[]{'y'});

        FileUtil.delete(folder);
        assertFalse(folder.exists());

        // 再删一次不应抛异常
        FileUtil.delete(folder);
        FileUtil.delete(null);
    }
}
