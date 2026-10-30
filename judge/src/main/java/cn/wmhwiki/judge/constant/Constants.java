/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.constant;

import java.io.File;

/**
 * 常量类
 */
public class Constants {

    // 协议版本
    public static final String protocolVersion = "1.0";

    // 模式：判题（沙箱不读取标准输出，仅执行）
    public static final String MODE_JUDGE = "judge";

    // 模式：仅运行代码（与 judge 行为相同，保留字段便于后续扩展）
    public static final String MODE_RUN = "run";

    // 容器数据卷路径
    public static final String containerVolumePath = "/data";

    // 评测代码根文件夹
    //
    // 关键约束：判题容器由**宿主机 Docker 守护进程**创建，Bind 的源路径由宿主机解释。
    // 因此这里的路径必须同时满足两点：
    //   1) 在 judge 进程内可写（源码要写在这里）；
    //   2) 在宿主机上是同一路径（守护进程按它去挂载）。
    // 本地直接跑 judge 时两者天然一致；容器化部署时用 JUDGE_SOURCE_DIR 显式对齐，
    // 并把同一路径同时挂给 judge 容器（详见 docker-compose.yml）。
    public static final String codeSourcePath = resolveCodeSourcePath();

    private static String resolveCodeSourcePath() {
        String configured = System.getenv("JUDGE_SOURCE_DIR");
        if (configured != null && !configured.trim().isEmpty()) {
            return configured.trim();
        }
        return System.getProperty("user.dir") + File.separator + "source";
    }

    // 默认编译时间限制 (ms)
    public static final Long compileTimeLimit = 5000L;
    public static final long minCompileTimeLimitMs = 1000L;
    public static final long maxCompileTimeLimitMs = 30000L;

    // 超时后继续执行时间的余量 (ms)，用于等待管道刷新
    public static final Long tleTimeLimit = 200L;

    // 默认时间/内存限制
    public static final long defaultTimeLimitMs = 1000L;
    public static final long minTimeLimitMs = 200L;
    public static final long maxTimeLimitMs = 5000L;
    public static final long defaultMemoryLimitMb = 256L;
    public static final long minMemoryLimitMb = 32L;
    public static final long maxMemoryLimitMb = 512L;

    // 最多测试点数
    public static final int maxTestCases = 10;

    // 单个测试输入最大长度（字符）
    public static final int maxInputLength = 64 * 1024;

    // 代码最大长度（字符）
    public static final int maxCodeLength = 64 * 1024;

    // 最大返回长度（字节），可由请求的 limits.maxOutputBytes 收紧
    public static final int maxCompileErrorLength = 4096;

    public static final int maxStdoutLength = 64 * 1024;

    public static final int maxStderrLength = 4096;

    // 容器最大进程数（防止 fork 炸弹）
    public static final long maxProcessCount = 64L;

    // 单个文件最大写入大小（字节，默认 16MB）
    public static final long maxOutputFileSize = 16L * 1024 * 1024;

    /**
     * 判题容器标签：用于定时清理时只处理本沙箱创建的容器。
     */
    public static final String containerLabelKey = "cn.wmhwiki.judge";

    public static final String containerLabelValue = "1";

    /**
     * 判题容器内运行用户。
     *
     * <p>镜像默认用户通常是 root，这里统一降权到 nobody。
     * 判题目录需对该 uid 可写（见 FileUtil#saveFile 的权限处理）。</p>
     */
    public static final String containerUser = "65534:65534";

    /**
     * 判题容器的常驻命令（PID 1）。
     *
     * <p>判题流程是：创建容器 → 在容器内 {@code exec} 编译 → {@code exec} 逐个测试点运行。
     * 这要求容器在两次 exec 之间一直存活。若不给 CMD，各语言镜像会用默认命令
     * （bash / python3 / node / sh / jshell）作为 PID 1，它们在没有 TTY、stdin 关闭时
     * 立刻退出，容器随之 Exited，下一次 exec 就会收到
     * {@code 409 Conflict: container ... is not running}。</p>
     *
     * <p>用 {@code sleep} 常驻：所有受支持镜像（debian 与 alpine 系）都带 sleep，
     * 空转不占 CPU，且被杀后不影响判题结果。</p>
     */
    public static final String[] containerKeepAliveCmd = {"sleep", "infinity"};

    /**
     * 单次判题允许的 CPU 秒数上限（兜底 wall clock 超时）
     */
    public static final long cpuSecondsLimit = 30L;
}
