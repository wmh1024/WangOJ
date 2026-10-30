/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.utils;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.attribute.PosixFilePermission;
import java.util.EnumSet;
import java.util.Set;

/**
 * 判题工作目录的文件操作。
 *
 * <p>判题容器以非 root 用户（默认 {@code nobody}）运行，而宿主机上写文件的进程用户不同，
 * 因此工作目录必须是所有人可写、可进入的，否则容器内无法写出编译产物。</p>
 */
public final class FileUtil {

    private static final Set<PosixFilePermission> DIR_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.OWNER_EXECUTE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.GROUP_WRITE,
            PosixFilePermission.GROUP_EXECUTE,
            PosixFilePermission.OTHERS_READ,
            PosixFilePermission.OTHERS_WRITE,
            PosixFilePermission.OTHERS_EXECUTE
    );

    /**
     * 目录最终权限（八进制 01777）：rwxrwxrwt
     */
    private static final int DIR_STICKY_MODE = 0b001_111_111_111;

    private static final Set<PosixFilePermission> FILE_PERMISSIONS = EnumSet.of(
            PosixFilePermission.OWNER_READ,
            PosixFilePermission.OWNER_WRITE,
            PosixFilePermission.GROUP_READ,
            PosixFilePermission.OTHERS_READ
    );

    private FileUtil() {
    }

    /**
     * 将数据写入文件，并确保目录对容器内用户可写。
     */
    public static File saveFile(String folderPath, String fileName, String content) {
        File folder = new File(folderPath);
        if (!folder.exists() && !folder.mkdirs()) {
            throw new IllegalStateException("创建目录失败：" + folderPath);
        }
        applyPermissions(folder.toPath(), DIR_PERMISSIONS);

        File file = new File(folder, fileName);
        // 显式使用 UTF-8：FileWriter 使用平台默认编码，非 UTF-8 宿主机上中文输入会乱码
        try (Writer writer = new OutputStreamWriter(new FileOutputStream(file), StandardCharsets.UTF_8)) {
            writer.write(content == null ? "" : content);
        } catch (IOException e) {
            throw new IllegalStateException("写入文件失败：" + file.getAbsolutePath(), e);
        }
        applyPermissions(file.toPath(), FILE_PERMISSIONS);
        return folder;
    }

    /**
     * 删除文件/文件夹（幂等）。
     */
    public static void delete(File folder) {
        if (folder == null || !folder.exists()) {
            return;
        }
        if (folder.isDirectory()) {
            File[] files = folder.listFiles();
            if (files != null) {
                for (File file : files) {
                    delete(file);
                }
            }
        }
        if (!folder.delete() && folder.exists()) {
            // 沙箱是独立进程，这里用 System.err 保持零依赖；调用方会记录 requestId
            System.err.println("删除失败：" + folder.getAbsolutePath());
        }
    }

    /**
     * 设置 POSIX 权限；非 POSIX 文件系统（如 Windows）忽略，不影响判题。
     *
     * <p>目录额外设置 sticky bit（01777）：容器用户（nobody）需要写权限来产出编译结果，
     * 但同一台机器上的其它本地用户不应能替换判题目录里的文件。</p>
     */
    private static void applyPermissions(java.nio.file.Path path, Set<PosixFilePermission> permissions) {
        try {
            if (path.getFileSystem().supportedFileAttributeViews().contains("posix")) {
                Files.setPosixFilePermissions(path, permissions);
                if (permissions == DIR_PERMISSIONS) {
                    // 八进制 01777：所有人可写 + 只有属主可删除/重命名目录内的文件
                    Files.setAttribute(path, "unix:mode", DIR_STICKY_MODE);
                }
            }
        } catch (Exception ignored) {
            // 权限设置失败不影响功能，容器内可能因此写失败，会在判题日志中体现
        }
    }
}
