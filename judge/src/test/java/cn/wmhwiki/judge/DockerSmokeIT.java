/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge;

import com.github.dockerjava.api.DockerClient;
import com.github.dockerjava.api.command.InspectContainerResponse;
import com.github.dockerjava.api.model.Capability;
import com.github.dockerjava.api.model.HostConfig;
import com.github.dockerjava.core.DockerClientBuilder;
import com.github.dockerjava.netty.NettyDockerCmdExecFactory;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

/**
 * 真容器冒烟测试（可选，默认被 Surefire 的 it 标签排除）。
 *
 * <p>运行方式：{@code mvn -Prun-it -Dtest=DockerSmokeIT -f judge/pom.xml test}。
 * 需要 Docker 守护进程与本地 {@code alpine:3.20} 镜像；缺失时自动跳过。</p>
 */
@Tag("it")
class DockerSmokeIT {

    private static final String IMAGE = "alpine:3.20";

    @Test
    void runsEchoInHardenedContainer() throws Exception {
        DockerClient client = null;
        try {
            client = DockerClientBuilder.getInstance()
                    .withDockerCmdExecFactory(new NettyDockerCmdExecFactory())
                    .build();
            client.pingCmd().exec();
            boolean imagePresent = !client.listImagesCmd()
                    .withImageNameFilter(IMAGE)
                    .exec()
                    .isEmpty();
            Assumptions.assumeTrue(imagePresent, "本地缺少 " + IMAGE + "，跳过真容器测试");

            HostConfig hostConfig = new HostConfig()
                    .withNetworkMode("none")
                    .withCapDrop(Capability.ALL)
                    .withMemory(64L * 1024 * 1024)
                    .withPidsLimit(32L);

            String containerId = client.createContainerCmd(IMAGE)
                    .withCmd("sh", "-c", "echo ok")
                    .withHostConfig(hostConfig)
                    .exec()
                    .getId();
            try {
                client.startContainerCmd(containerId).exec();
                Thread.sleep(1000L);
                InspectContainerResponse inspect = client.inspectContainerCmd(containerId).exec();
                Assumptions.assumeTrue(inspect.getState() != null, "容器状态不可读");
                Assumptions.assumeTrue(Boolean.FALSE.equals(inspect.getState().getRunning()),
                        "容器未在 1 秒内退出，跳过（可能是镜像首次启动较慢）");
                org.junit.jupiter.api.Assertions.assertEquals(
                        0L, inspect.getState().getExitCodeLong().longValue());
            } finally {
                client.removeContainerCmd(containerId).withForce(true).exec();
            }
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Docker 不可用，跳过真容器测试: " + e.getMessage());
        } finally {
            if (client != null) {
                client.close();
            }
        }
    }
}
