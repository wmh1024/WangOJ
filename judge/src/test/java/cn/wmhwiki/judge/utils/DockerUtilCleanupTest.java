/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.utils;

import cn.wmhwiki.judge.constant.Constants;
import com.github.dockerjava.api.model.ContainerConfig;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 孤儿容器识别单测：兼容新标签容器与升级前无标签的旧容器。
 */
class DockerUtilCleanupTest {

    @Test
    void labeledContainerIsOursEvenWithUnknownImage() {
        ContainerConfig config = new ContainerConfig()
                .withWorkingDir(Constants.containerVolumePath)
                .withImage("custom:latest")
                .withLabels(Collections.singletonMap(
                        Constants.containerLabelKey, Constants.containerLabelValue));

        assertTrue(DockerUtil.isJudgeContainer(config));
    }

    @Test
    void unlabeledLegacyContainerWithKnownImageIsOurs() {
        ContainerConfig config = new ContainerConfig()
                .withWorkingDir(Constants.containerVolumePath)
                .withImage("python:3.12-alpine");

        assertTrue(DockerUtil.isJudgeContainer(config));
    }

    @Test
    void unlabeledUnknownImageIsNotOurs() {
        ContainerConfig config = new ContainerConfig()
                .withWorkingDir(Constants.containerVolumePath)
                .withImage("nginx:latest");

        assertFalse(DockerUtil.isJudgeContainer(config));
    }

    @Test
    void otherWorkingDirectoryIsNotOurs() {
        ContainerConfig config = new ContainerConfig()
                .withWorkingDir("/tmp")
                .withImage("python:3.12-alpine")
                .withLabels(Collections.singletonMap(
                        Constants.containerLabelKey, Constants.containerLabelValue));

        assertFalse(DockerUtil.isJudgeContainer(config));
        assertFalse(DockerUtil.isJudgeContainer(null));
    }
}
