/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.utils;

import cn.wmhwiki.judge.constant.Constants;
import cn.wmhwiki.judge.constant.enums.LangContextEnum;
import cn.wmhwiki.judge.pojo.dto.JudgeLimits;
import cn.wmhwiki.judge.pojo.lang.LangContext;
import cn.wmhwiki.judge.pojo.vo.JudgeCaseResult;
import cn.wmhwiki.judge.pojo.vo.JudgeResponse;
import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.async.ResultCallback;
import com.github.dockerjava.api.command.ExecCreateCmdResponse;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.command.InspectExecResponse;
import com.github.dockerjava.api.command.StatsCmd;
import com.github.dockerjava.api.model.Bind;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.ContainerConfig;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.api.model.Statistics;
import com.github.dockerjava.api.model.Ulimit;
import com.github.dockerjava.api.model.Volume;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.core.command.ExecStartResultCallback;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.ByteArrayOutputStream;
import java.io.Closeable;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Docker 判题执行工具（协议 v1.0 实现）。
 *
 * <p>一次请求只创建一个容器：编译一次，每个测试点通过独立的 exec 执行；
 * 统一设置超时、超时后 kill、内存采样、输出截断和资源清理。</p>
 */
public final class DockerUtil {

    private static final Logger log = LoggerFactory.getLogger(DockerUtil.class);

    // 单测试点执行层面的结论
    private static final String VERDICT_ACCEPTED = "ACCEPTED";
    private static final String VERDICT_TLE = "TIME_LIMIT_EXCEEDED";
    private static final String VERDICT_MLE = "MEMORY_LIMIT_EXCEEDED";
    private static final String VERDICT_RE = "RUNTIME_ERROR";
    private static final String VERDICT_OLE = "OUTPUT_LIMIT_EXCEEDED";
    private static final String VERDICT_SE = "SYSTEM_ERROR";

    /**
     * 进程被信号杀死时的退出码：128 + signal。
     *
     * <p>137 = SIGKILL（cgroup OOM 或 kill -9）、143 = SIGTERM、134 = SIGABRT（JVM OOM 常见）。
     * 三者都可能由"内存超限"引起，需要结合内存采样共同判断。</p>
     */
    private static final int EXIT_KILLED = 137;
    private static final int EXIT_SIGABRT = 134;
    private static final int EXIT_SIGTERM = 143;

    /**
     * 当前进程正在使用的容器 ID。
     *
     * <p>定时清理不得删除正在判题的容器；进程重启后集合为空，
     * 上次崩溃遗留的容器会被当作孤儿清理。</p>
     */
    private static final Set<String> ACTIVE_CONTAINERS =
            Collections.newSetFromMap(new ConcurrentHashMap<String, Boolean>());

    /**
     * 判题语言镜像集合：用于兼容升级前没有标签的旧容器。
     */
    private static final Set<String> JUDGE_IMAGES;

    static {
        Set<String> images = new HashSet<String>();
        for (LangContextEnum item : LangContextEnum.values()) {
            images.add(item.getLangContext().getImage());
        }
        JUDGE_IMAGES = Collections.unmodifiableSet(images);
    }

    private DockerUtil() {
    }

    public static JudgeResponse execute(String requestId, LangContext langContext, JudgeLimits limits,
                                        int testCaseCount, String folderPath) {
        JudgeResponse response = JudgeResponse.success(requestId);
        DockerClient dockerClient = DockerClientBuilder.getInstance()
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                .build();
        String containerId = null;
        try {
            containerId = createAndStartContainer(dockerClient, langContext.getImage(),
                    limits.getMemoryLimitMb() * 1024 * 1024, folderPath);

            // 编译（编译命令为内部常量模板，不经过 shell）
            String sourcePath = Constants.containerVolumePath + File.separator
                    + langContext.getSourceFileName();
            String[] compileCmd = langContext.buildCompileCmd(sourcePath);
            if (compileCmd != null) {
                String compileError = compile(dockerClient, compileCmd, containerId,
                        limits.getCompileTimeLimitMs());
                if (compileError != null) {
                    response.setVerdict(JudgeResponse.VERDICT_COMPILE_ERROR);
                    response.setMessage("compilation error");
                    response.setCompileOutput(truncate(compileError, Constants.maxCompileErrorLength));
                    response.setCases(new ArrayList<JudgeCaseResult>());
                    response.setTotalTimeMs(0L);
                    response.setMaxMemoryBytes(0L);
                    return response;
                }
            }

            // 逐个测试点执行：启动命令按本题内存限制构造（Java 会追加 -Xmx）
            String[] runCmd = langContext.buildRunCmd(limits.getMemoryLimitMb());
            List<JudgeCaseResult> cases = new ArrayList<JudgeCaseResult>(testCaseCount);
            long totalTimeMs = 0L;
            long maxMemoryBytes = 0L;
            for (int i = 0; i < testCaseCount; i++) {
                File in = new File(folderPath + File.separator + i + ".in");
                JudgeCaseResult caseResult = executeOne(dockerClient, langContext, runCmd, containerId,
                        limits, i, in);
                cases.add(caseResult);
                totalTimeMs += caseResult.getTimeMs() == null ? 0L : caseResult.getTimeMs();
                maxMemoryBytes = Math.max(maxMemoryBytes,
                        caseResult.getMemoryBytes() == null ? 0L : caseResult.getMemoryBytes());
            }
            response.setCases(cases);
            response.setTotalTimeMs(totalTimeMs);
            response.setMaxMemoryBytes(maxMemoryBytes);
            return response;
        } catch (JudgeSystemException e) {
            log.error("判题系统错误, requestId={}, reason={}", requestId, e.getMessage());
            return JudgeResponse.error(requestId, 500, e.getMessage());
        } finally {
            if (containerId != null) {
                deleteContainer(dockerClient, containerId);
                ACTIVE_CONTAINERS.remove(containerId);
            }
            closeQuietly(dockerClient);
        }
    }

    /**
     * 创建并启动容器。
     *
     * <p>沙箱必须假设容器里运行的是不可信代码，因此：</p>
     * <ul>
     *   <li>禁用网络；</li>
     *   <li>限制内存与 swap（相等即禁止 swap）；</li>
     *   <li>限制 CPU 与 PID 数量（防 fork 炸弹）；</li>
     *   <li>{@code cap-drop ALL} + {@code no-new-privileges}；</li>
     *   <li>限制进程数、句柄数、单文件大小、core dump；</li>
     *   <li>工作目录只挂载本次判题目录；</li>
     *   <li>私有 PID namespace（避免 kill -1 波及宿主机）。</li>
     * </ul>
     *
     * <p>注意：未启用 readonly rootfs —— 编译/运行时需要写临时目录（/tmp、/root/.cache 等），
     * 只读根文件系统会导致 gcc、java、node 等运行时失败。</p>
     */
    private static String createAndStartContainer(DockerClient dockerClient, String image,
                                                  long memoryLimitBytes, String folderPath) {
        HostConfig hostConfig = new HostConfig();
        hostConfig.withMemory(memoryLimitBytes);
        hostConfig.withMemorySwap(memoryLimitBytes);      // 禁止使用 swap 绕过内存限制
        hostConfig.withCpuCount(1L);
        hostConfig.withPidsLimit(Constants.maxProcessCount);
        hostConfig.withCapDrop(Capability.ALL);
        hostConfig.withSecurityOpts(Arrays.asList("no-new-privileges"));
        // 私有 PID namespace：容器内的 kill -1 只影响容器自身
        hostConfig.withPidMode("");
        // 由内核在内存超限时终止进程（不依赖用户态 OOM killer）
        hostConfig.withOomKillDisable(false);
        hostConfig.withUlimits(Arrays.asList(
                new Ulimit("FSIZE", Constants.maxOutputFileSize, Constants.maxOutputFileSize),
                new Ulimit("NOFILE", 256L, 256L),
                new Ulimit("NPROC", Constants.maxProcessCount, Constants.maxProcessCount),
                new Ulimit("CORE", 0L, 0L),
                new Ulimit("CPU", Constants.cpuSecondsLimit, Constants.cpuSecondsLimit)
        ));
        hostConfig.setBinds(new Bind(folderPath, new Volume(Constants.containerVolumePath)));
        String containerId = null;
        try {
            containerId = dockerClient.createContainerCmd(image)
                    .withHostConfig(hostConfig)
                    .withWorkingDir(Constants.containerVolumePath)
                    .withNetworkDisabled(true)
                    // 容器以非 root 用户运行：镜像的默认用户通常是 root，这里强降到 nobody
                    .withUser(Constants.containerUser)
                    // 必须给一个长期驻留的 PID 1：判题靠 exec 在容器内跑编译/运行命令，
                    // 而各语言镜像的默认 CMD（bash / python3 / node / sh / jshell）在无 TTY、
                    // 无 stdin 时会立刻退出，容器随即 Exited，后续 exec 就报 409
                    // "container ... is not running"。用 sleep 常驻，开销可忽略。
                    .withCmd(Constants.containerKeepAliveCmd)
                    .withLabels(Collections.singletonMap(Constants.containerLabelKey, Constants.containerLabelValue))
                    .withAttachStdin(true)
                    .withAttachStderr(true)
                    .withAttachStdout(true)
                    .withTty(false)
                    .exec()
                    .getId();
            ACTIVE_CONTAINERS.add(containerId);
            dockerClient.startContainerCmd(containerId).exec();
        } catch (Exception e) {
            if (containerId != null) {
                ACTIVE_CONTAINERS.remove(containerId);
                deleteContainer(dockerClient, containerId);
            }
            throw new JudgeSystemException("创建判题容器失败（镜像 " + image + " 是否已拉取？）: " + e.getMessage());
        }
        return containerId;
    }

    private static void deleteContainer(DockerClient dockerClient, String containerId) {
        try {
            dockerClient.removeContainerCmd(containerId).withForce(true).withRemoveVolumes(true).exec();
        } catch (Exception e) {
            log.warn("删除容器失败: {}, {}", containerId, e.getMessage());
        }
    }

    private static ExecCreateCmdResponse createExec(DockerClient dockerClient, String containerId, String[] cmdArray) {
        return dockerClient.execCreateCmd(containerId)
                .withCmd(cmdArray)
                .withAttachStdin(true)
                .withAttachStderr(true)
                .withAttachStdout(true)
                .exec();
    }

    /**
     * 编译代码。
     *
     * @param compileCmd 已展开的编译命令数组，不经过 shell，也不做空格切分
     * @return 编译错误信息；成功返回 null
     */
    private static String compile(DockerClient dockerClient, String[] compileCmd, String containerId,
                                  long compileTimeLimitMs) {
        ExecCreateCmdResponse execResponse = createExec(dockerClient, containerId, compileCmd);
        String execId = execResponse.getId();

        ByteArrayOutputStream stdout = new ByteArrayOutputStream();
        ByteArrayOutputStream stderr = new ByteArrayOutputStream();
        boolean finished;
        try {
            finished = dockerClient.execStartCmd(execId)
                    .exec(new ExecStartResultCallback(stdout, stderr))
                    .awaitCompletion(compileTimeLimitMs, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new JudgeSystemException("编译被中断");
        }
        if (!finished) {
            killExecQuietly(dockerClient, containerId, execId);
            return "编译超时（超过 " + compileTimeLimitMs + "ms）";
        }
        Integer exitCode = inspectExitCodeQuietly(dockerClient, execId);
        if (exitCode != null && exitCode != 0) {
            String errorText = new String(stderr.toByteArray(), StandardCharsets.UTF_8);
            if (errorText.trim().isEmpty()) {
                errorText = new String(stdout.toByteArray(), StandardCharsets.UTF_8);
            }
            return truncate(errorText, Constants.maxCompileErrorLength);
        }
        return null;
    }

    /**
     * 执行单个测试点。
     *
     * <p>超时后主动 kill 进程，避免死循环代码继续占用 CPU 或影响后续测试点；
     * 输出超过上限时由看门狗线程终止进程，避免用户代码持续刷 stdout 打爆沙箱内存。</p>
     */
    private static JudgeCaseResult executeOne(DockerClient dockerClient, LangContext langContext,
                                              String[] runCmd, String containerId,
                                              JudgeLimits limits, int index, File in) {
        long maxOutputBytes = Math.min(limits.getMaxOutputBytes(), Constants.maxStdoutLength);
        LimitedOutputStream stdout = new LimitedOutputStream((int) maxOutputBytes);
        LimitedOutputStream stderr = new LimitedOutputStream(Constants.maxStderrLength);
        ExecCreateCmdResponse execResponse = createExec(dockerClient, containerId, runCmd);
        String execId = execResponse.getId();

        // 内存采样：在测试点执行期间持续订阅 docker stats，而不是只取一次快照
        AtomicLong memoryBytes = new AtomicLong(0L);
        AtomicBoolean memorySampled = new AtomicBoolean(false);
        StatsCollector statsCollector = new StatsCollector(containerId, memoryBytes, memorySampled);
        statsCollector.start(dockerClient);

        // 输出看门狗：一旦超过上限立即终止进程，让 awaitCompletion 尽快返回
        OutputWatchdog watchdog = new OutputWatchdog(stdout, dockerClient, containerId, execId);
        watchdog.start();

        String verdict;
        long startNano = System.nanoTime();
        boolean finished = false;
        boolean startFailed = false;
        try (InputStream inputStream = new FileInputStream(in)) {
            finished = dockerClient.execStartCmd(execId)
                    .withStdIn(inputStream)
                    .exec(new ExecStartResultCallback(stdout, stderr))
                    .awaitCompletion(limits.getTimeLimitMs() + Constants.tleTimeLimit, TimeUnit.MILLISECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            startFailed = true;
        } catch (Exception e) {
            log.warn("执行测试点失败, containerId={}, index={}: {}", containerId, index, e.getMessage());
            startFailed = true;
        } finally {
            watchdog.stop();
            statsCollector.stop();
        }
        long elapsedMs = (System.nanoTime() - startNano) / 1_000_000;
        Long sampledMemoryBytes = memorySampled.get() ? Long.valueOf(memoryBytes.get()) : null;
        // docker stats 对流式采样存在竞争窗口：几十毫秒就跑完的程序往往一帧都收不到，
        // 此时内存会记为 null，后端会把「沙箱声称 ACCEPTED 但无内存数据」判成 SYSTEM_ERROR。
        // cgroup 的 memory.peak 由内核记录、进程退出后仍在，读一次即可，不受采样时机影响。
        // 注意它是容器级累计峰值（单调不减），直接取绝对值即可：同一容器内多个测试点
        // 单调递增，用 max 合并不会低估任何一个测试点。
        Long peakBytes = readCgroupPeakBytes(dockerClient, containerId);
        if (peakBytes != null && (sampledMemoryBytes == null || peakBytes > sampledMemoryBytes)) {
            sampledMemoryBytes = peakBytes;
        }

        if (startFailed) {
            verdict = VERDICT_SE;
        } else if (watchdog.isKilled()) {
            verdict = VERDICT_OLE;
        } else if (!finished) {
            killExecQuietly(dockerClient, containerId, execId);
            verdict = VERDICT_TLE;
        } else {
            verdict = resolveVerdict(dockerClient, containerId, execId, sampledMemoryBytes, limits,
                    langContext, stderr.asString());
        }
        return buildResult(index, verdict, stdout, stderr, elapsedMs, sampledMemoryBytes);
    }

    private static JudgeCaseResult buildResult(int index, String verdict, LimitedOutputStream stdout,
                                               LimitedOutputStream stderr, long elapsedMs, Long memoryBytes) {
        JudgeCaseResult result = new JudgeCaseResult();
        result.setIndex(index);
        result.setVerdict(verdict);
        result.setStdout(stdout.asString());
        result.setStderr(stderr.asString());
        result.setTimeMs(elapsedMs);
        result.setMemoryBytes(memoryBytes);
        return result;
    }

    private static String resolveVerdict(DockerClient dockerClient, String containerId, String execId,
                                         Long memoryBytes, JudgeLimits limits, LangContext langContext,
                                         String stderr) {
        Integer exitCode = inspectExitCodeQuietly(dockerClient, execId);
        if (exitCode == null) {
            return VERDICT_SE;
        }
        if (memoryBytes != null) {
            long memoryLimitBytes = limits.getMemoryLimitMb() * 1024 * 1024;
            if (memoryBytes > memoryLimitBytes) {
                return VERDICT_MLE;
            }
        }
        return mapExitCode(exitCode, langContext.isJvmOutOfMemoryExit(exitCode, stderr));
    }

    /**
     * 退出码到测试点结论的映射（与 Docker 解耦，便于单测）。
     *
     * <p>137 = SIGKILL（通常为 cgroup OOM）；134 = SIGABRT（JVM OOM 常见）；143 = SIGTERM；
     * 另外，语言上下文可声明该退出为堆内存超限（如 Java 的 {@code ExitOnOutOfMemoryError}）。</p>
     */
    static String mapExitCode(int exitCode, boolean jvmOutOfMemory) {
        if (exitCode == 0) {
            return VERDICT_ACCEPTED;
        }
        if (jvmOutOfMemory) {
            return VERDICT_MLE;
        }
        if (exitCode == EXIT_KILLED || exitCode == EXIT_SIGABRT || exitCode == EXIT_SIGTERM) {
            return VERDICT_MLE;
        }
        return VERDICT_RE;
    }

    private static Integer inspectExitCodeQuietly(DockerClient dockerClient, String execId) {
        try {
            InspectExecResponse inspect = dockerClient.inspectExecCmd(execId).exec();
            return inspect.getExitCode();
        } catch (Exception e) {
            log.warn("读取退出码失败, execId={}: {}", execId, e.getMessage());
            return null;
        }
    }

    /**
     * 超时后终止进程。
     *
     * <p>先 kill 该 exec 对应的进程组（避免子进程残留），失败时兜底 kill 容器内所有进程。
     * 容器本身在请求结束时会被强制删除，这里的目标是让沙箱尽快停止占用 CPU。</p>
     */
    private static void killExecQuietly(DockerClient dockerClient, String containerId, String execId) {
        Long pid = null;
        try {
            InspectExecResponse inspect = dockerClient.inspectExecCmd(execId).exec();
            pid = inspect.getPidLong();
        } catch (Exception ignored) {
            // 进程可能已退出
        }
        if (pid != null && pid > 0) {
            if (execQuietly(dockerClient, containerId, new String[]{"kill", "-9", String.valueOf(pid)})) {
                return;
            }
            // 进程组：-pid
            if (execQuietly(dockerClient, containerId,
                    new String[]{"sh", "-c", "kill -9 -" + pid + " 2>/dev/null || kill -9 " + pid})) {
                return;
            }
        }
        // 兜底：杀掉容器内所有进程（容器使用私有 PID namespace，不会影响宿主机）
        execQuietly(dockerClient, containerId, new String[]{"sh", "-c", "kill -9 -1 2>/dev/null || true"});
    }

    /**
     * 在容器内执行一条命令，超时 1 秒，忽略所有输出与异常。
     *
     * @return 是否执行成功（未超时）
     */
    private static boolean execQuietly(DockerClient dockerClient, String containerId, String[] command) {
        try {
            ExecCreateCmdResponse exec = createExec(dockerClient, containerId, command);
            return dockerClient.execStartCmd(exec.getId())
                    .exec(new ExecStartResultCallback())
                    .awaitCompletion(1, TimeUnit.SECONDS);
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * 读取容器 cgroup 的**峰值**内存（cgroup v2 {@code memory.peak}）。
     *
     * <p>为什么需要它：{@link StatsCollector} 通过 docker stats 流式采样，
     * 而绝大多数判题程序在几十毫秒内就跑完，采样线程还没来得及收到一帧，
     * 于是 {@code memorySampled} 一直是 false、内存记为 null。后端对
     * "沙箱声称 ACCEPTED 但没有内存数据"的处理是判为 SYSTEM_ERROR
     * （见 JudgeOutputUtils.resolveCaseVerdict），结果就是**短暂程序全部误判为系统错误**。</p>
     *
     * <p>{@code memory.peak} 由内核记录、在进程退出后仍然保留，读一次即可拿到
     * 该测试点的真实峰值，不受采样时机影响。容器级 counter 会跨测试点累计，
     * 所以调用方按"取增量"的方式使用（见 {@code executeOne}）。</p>
     *
     * @return 峰值字节数；读取失败或内核不支持时返回 null（不伪造 0）
     */
    private static Long readCgroupPeakBytes(DockerClient dockerClient, String containerId) {
        String[] candidates = {
                "/sys/fs/cgroup/memory.peak",
                "/sys/fs/cgroup/memory/memory.max_usage_in_bytes"
        };
        for (String path : candidates) {
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            try {
                ExecCreateCmdResponse exec = createExec(dockerClient, containerId,
                        new String[]{"cat", path});
                boolean finished = dockerClient.execStartCmd(exec.getId())
                        .exec(new ExecStartResultCallback(out, new ByteArrayOutputStream()))
                        .awaitCompletion(1, TimeUnit.SECONDS);
                if (!finished) {
                    continue;
                }
                String text = new String(out.toByteArray(), StandardCharsets.UTF_8).trim();
                if (!text.isEmpty()) {
                    long value = Long.parseLong(text);
                    if (value > 0) {
                        return value;
                    }
                }
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return null;
            } catch (Exception e) {
                // 内核不支持该文件或读取失败：继续尝试下一个候选
            }
        }
        return null;
    }

    /**
     * 清理指定沙箱容器的孤儿实例（容器工作目录为沙箱挂载点时认为属于本沙箱）。
     *
     * @return 清理的容器数量
     */
    public static int cleanupOrphanContainers() {
        DockerClient dockerClient = DockerClientBuilder.getInstance()
                .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                .build();
        int cleaned = 0;
        try {
            for (com.github.dockerjava.api.model.Container container : dockerClient.listContainersCmd()
                    .withShowAll(true).exec()) {
                InspectContainerResponse inspect;
                try {
                    inspect = dockerClient.inspectContainerCmd(container.getId()).exec();
                } catch (Exception e) {
                    // 容器可能在列举与 inspect 之间退出，跳过即可
                    continue;
                }
                if (inspect.getConfig() == null) {
                    continue;
                }
                // 只处理本沙箱创建的容器，且跳过当前进程正在判题的容器。
                if (!isJudgeContainer(inspect.getConfig())
                        || ACTIVE_CONTAINERS.contains(inspect.getId())) {
                    continue;
                }
                deleteContainer(dockerClient, inspect.getId());
                cleaned++;
            }
            if (cleaned > 0) {
                log.info("已清理 {} 个孤儿判题容器", cleaned);
            }
        } catch (Exception e) {
            log.warn("清理孤儿容器失败: {}", e.getMessage());
        } finally {
            closeQuietly(dockerClient);
        }
        return cleaned;
    }

    /**
     * 判断容器是否属于判题沙箱。
     *
     * <p>新容器带 {@code cn.wmhwiki.judge=1} 标签；升级前创建的旧容器没有标签，
     * 通过“工作目录 + 已知判题镜像”兼容识别，避免升级后永久残留。</p>
     */
    static boolean isJudgeContainer(ContainerConfig config) {
        if (config == null || !Constants.containerVolumePath.equals(config.getWorkingDir())) {
            return false;
        }
        Map<String, String> labels = config.getLabels();
        boolean labeled = labels != null
                && Constants.containerLabelValue.equals(labels.get(Constants.containerLabelKey));
        return labeled || JUDGE_IMAGES.contains(config.getImage());
    }

    private static void closeQuietly(Closeable closeable) {
        try {
            if (closeable != null) {
                closeable.close();
            }
        } catch (Exception ignored) {
            // ignore
        }
    }

    private static String truncate(String text, int maxLength) {
        if (text == null) {
            return "";
        }
        if (text.length() <= maxLength) {
            return text;
        }
        return text.substring(0, maxLength);
    }

    /**
     * 有上限的字节缓冲区：超过上限后只记录"已超限"，不再继续累积，避免用户代码刷爆沙箱内存。
     */
    static final class LimitedOutputStream extends ByteArrayOutputStream {

        private final int limit;

        private final AtomicBoolean exceeded = new AtomicBoolean(false);

        LimitedOutputStream(int limit) {
            super(Math.min(limit, 8192));
            this.limit = limit;
        }

        @Override
        public synchronized void write(int b) {
            if (size() >= limit) {
                exceeded.set(true);
                return;
            }
            super.write(b);
        }

        @Override
        public synchronized void write(byte[] b, int off, int len) {
            int remaining = limit - size();
            if (remaining <= 0) {
                exceeded.set(true);
                return;
            }
            if (len > remaining) {
                exceeded.set(true);
                super.write(b, off, remaining);
                return;
            }
            super.write(b, off, len);
        }

        boolean isExceeded() {
            return exceeded.get();
        }

        String asString() {
            return new String(toByteArray(), StandardCharsets.UTF_8);
        }
    }

    /**
     * 输出看门狗：轮询输出缓冲区，一旦超过上限就 kill 掉 exec。
     *
     * <p>不使用"在 onNext 里抛异常"的方式：docker-java 的回调线程捕获异常后
     * awaitCompletion 不一定会及时返回，进程也可能继续跑满整个时间限制。</p>
     */
    private static final class OutputWatchdog {

        private static final long POLL_INTERVAL_MS = 50L;

        private final LimitedOutputStream stdout;
        private final DockerClient dockerClient;
        private final String containerId;
        private final String execId;
        private final AtomicBoolean killed = new AtomicBoolean(false);
        private final AtomicBoolean running = new AtomicBoolean(true);
        private Thread thread;

        private OutputWatchdog(LimitedOutputStream stdout, DockerClient dockerClient,
                               String containerId, String execId) {
            this.stdout = stdout;
            this.dockerClient = dockerClient;
            this.containerId = containerId;
            this.execId = execId;
        }

        void start() {
            thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (running.get()) {
                        if (stdout.isExceeded()) {
                            killed.set(true);
                            log.info("输出超过上限，终止进程, containerId={}, execId={}", containerId, execId);
                            killExecQuietly(dockerClient, containerId, execId);
                            return;
                        }
                        try {
                            Thread.sleep(POLL_INTERVAL_MS);
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                            return;
                        }
                    }
                }
            }, "judge-output-watchdog");
            thread.setDaemon(true);
            thread.start();
        }

        void stop() {
            running.set(false);
            if (thread != null) {
                thread.interrupt();
                thread = null;
            }
        }

        boolean isKilled() {
            return killed.get();
        }
    }

    /**
     * 内存采样器：在测试点执行期间持续订阅 docker stats，取本次测试点的采样峰值。
     *
     * <p>使用 {@code usage} 而不是容器生命周期的 {@code maxUsage}，避免前一个测试点的峰值
     * 污染后续测试点。采样不到时不伪造 0，而是保持 null，让后端按“未知”处理。</p>
     */
    private static final class StatsCollector {

        private final String containerId;
        private final AtomicLong memoryBytes;
        private final AtomicBoolean sampled;
        private StatsCmd statsCmd;
        private ResultCallback<Statistics> callback;

        private StatsCollector(String containerId, AtomicLong memoryBytes, AtomicBoolean sampled) {
            this.containerId = containerId;
            this.memoryBytes = memoryBytes;
            this.sampled = sampled;
        }

        void start(DockerClient dockerClient) {
            try {
                statsCmd = dockerClient.statsCmd(containerId);
                callback = new ResultCallback.Adapter<Statistics>() {
                    @Override
                    public void onNext(Statistics statistics) {
                        if (statistics == null || statistics.getMemoryStats() == null) {
                            return;
                        }
                        Long usage = statistics.getMemoryStats().getUsage();
                        if (usage != null && usage > 0) {
                            sampled.set(true);
                            memoryBytes.set(Math.max(memoryBytes.get(), usage));
                        }
                    }

                    @Override
                    public void onError(Throwable throwable) {
                        log.warn("内存采样失败, containerId={}: {}", containerId, throwable.getMessage());
                    }
                };
                statsCmd.exec(callback);
            } catch (Exception e) {
                log.warn("内存采样不可用, containerId={}: {}", containerId, e.getMessage());
                closeQuietly(callback);
                callback = null;
                statsCmd = null;
            }
        }

        void stop() {
            closeQuietly(callback);
            callback = null;
            statsCmd = null;
        }
    }

    /**
     * 沙箱内部的系统级失败，转换为协议 SYSTEM_ERROR。
     */
    public static class JudgeSystemException extends RuntimeException {
        public JudgeSystemException(String message) {
            super(message);
        }
    }
}
