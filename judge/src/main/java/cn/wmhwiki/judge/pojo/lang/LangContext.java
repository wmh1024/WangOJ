/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.pojo.lang;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class LangContext implements Serializable {

    private static final long serialVersionUID = 1L;

    /**
     * 编译命令模板中的源文件占位符。
     *
     * <p>编译命令以字符串数组保存，不再用 {@code String.format(...).split(" ")}，
     * 避免参数带空格时被静默拆错（见 m-7）。</p>
     */
    public static final String SOURCE_PLACEHOLDER = "{source}";

    // 编程语言
    private String lang;

    // 保存文件名
    private String sourceFileName;

    // 编程语言对应的docker镜像
    private String image;

    // 编程语言对应执行的编译命令模板（null 表示无需编译）
    private String[] compileCmd;

    // 编程语言对应执行的启动命令
    private String[] runCmd;

    /**
     * 根据内存限制构造启动命令。
     *
     * <p>默认返回固定 {@code runCmd} 的副本，调用方可以安全修改返回值；
     * 子类可覆写以按语言追加运行时参数（例如 Java 的 {@code -Xmx}）。</p>
     *
     * <p>注意：{@code LangContextEnum} 持有单例上下文对象，实现中不得修改自身字段。</p>
     */
    public String[] buildRunCmd(long memoryLimitMb) {
        String[] cmd = getRunCmd();
        return cmd == null ? null : cmd.clone();
    }

    /**
     * 按源文件路径展开编译命令模板。
     *
     * @return 可直接交给 {@code docker exec} 的命令数组；无需编译时返回 {@code null}
     */
    public String[] buildCompileCmd(String sourcePath) {
        String[] template = getCompileCmd();
        if (template == null) {
            return null;
        }
        String[] cmd = template.clone();
        for (int i = 0; i < cmd.length; i++) {
            if (SOURCE_PLACEHOLDER.equals(cmd[i])) {
                cmd[i] = sourcePath;
            }
        }
        return cmd;
    }

    /**
     * 是否应将退出码判定为 JVM 堆内存超限。
     *
     * <p>默认 {@code false}；Java 上下文覆写后，{@code -XX:+ExitOnOutOfMemoryError}
     * 的退出码 3（或 stderr 中出现 OutOfMemoryError）会被映射为 MLE。</p>
     */
    public boolean isJvmOutOfMemoryExit(int exitCode, String stderr) {
        return false;
    }

}
