/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.lang;

import cn.wmhwiki.judge.constant.Constants;
import lombok.Data;
import lombok.EqualsAndHashCode;


@Data
@EqualsAndHashCode(callSuper = false)
public class JavaContext extends LangContext {

    /** JVM {@code -XX:+ExitOnOutOfMemoryError} 的约定退出码。 */
    private static final int JVM_OOM_EXIT_CODE = 3;

    // 编程语言
    private final String lang = "java";

    // 保存文件名
    private final String sourceFileName = "Main.java";

    // 编程语言对应的docker镜像
    private final String image = "eclipse-temurin:17-jdk-alpine";

    // 编程语言对应执行的编译命令
    private final String[] compileCmd = {
            "javac", "-encoding", "utf-8", LangContext.SOURCE_PLACEHOLDER
    };

    // 编程语言对应执行的启动命令
    private final String[] runCmd = {"java", "-cp", Constants.containerVolumePath, "Main"};

    /**
     * 按题目内存限制设置 JVM 堆上限。
     *
     * <p>容器内存限制包含堆、元空间、线程栈与 JVM 自身开销，堆取 3/4 留出余量；
     * 最小值 16MB 避免异常小的限制产生非法参数。{@code ExitOnOutOfMemoryError}
     * 让堆 OOM 以退出码 3 结束，便于沙箱稳定判定 MLE。</p>
     */
    @Override
    public String[] buildRunCmd(long memoryLimitMb) {
        long heapMb = Math.max(16L, memoryLimitMb * 3 / 4);
        return new String[]{
                "java", "-Xmx" + heapMb + "m", "-XX:+UseSerialGC",
                "-XX:+ExitOnOutOfMemoryError", "-cp", Constants.containerVolumePath, "Main"
        };
    }

    @Override
    public boolean isJvmOutOfMemoryExit(int exitCode, String stderr) {
        if (exitCode == 0) {
            return false;
        }
        // ExitOnOutOfMemoryError 的约定退出码为 3；另以 stderr 兜底，覆盖 JVM
        // 未按约定退出（例如被包装）的情况。注意：用户程序自行 exit(3)
        // 也会被视为 MLE，这是启用退出码标记的已知代价。
        return exitCode == JVM_OOM_EXIT_CODE
                || (stderr != null && stderr.contains("OutOfMemoryError"));
    }
}
