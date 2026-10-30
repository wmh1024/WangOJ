/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.task;

import cn.wmhwiki.judge.utils.DockerUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicBoolean;

/**
 * 定时清理崩溃遗留的判题容器。
 *
 * <p>启动后立即执行一次，之后每 5 分钟执行一次。正在判题的容器由
 * {@link DockerUtil#cleanupOrphanContainers()} 主动跳过，不会被误杀。</p>
 */
@Component
public class ContainerCleanupTask {

    private static final Logger log = LoggerFactory.getLogger(ContainerCleanupTask.class);

    private final AtomicBoolean running = new AtomicBoolean(false);

    @Scheduled(initialDelay = 0L, fixedDelay = 300_000L)
    public void cleanup() {
        if (!running.compareAndSet(false, true)) {
            return;
        }
        try {
            DockerUtil.cleanupOrphanContainers();
        } catch (Exception e) {
            // cleanupOrphanContainers 自身已吞异常，这里只兜底防止调度线程退出
            log.warn("定时清理孤儿容器失败: {}", e.getMessage());
        } finally {
            running.set(false);
        }
    }
}
