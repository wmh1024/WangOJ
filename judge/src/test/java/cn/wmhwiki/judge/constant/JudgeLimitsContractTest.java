/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge.constant;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;

/**
 * 跨模块契约：沙箱边界常量必须与仓库根目录 judge-limits.json 一致。
 */
class JudgeLimitsContractTest {

    private static JsonObject readContract() throws IOException {
        Path[] candidates = {
                Paths.get("judge-limits.json"),
                Paths.get("..", "judge-limits.json")
        };
        for (Path candidate : candidates) {
            if (Files.exists(candidate)) {
                return JsonParser.parseString(
                        new String(Files.readAllBytes(candidate), StandardCharsets.UTF_8)).getAsJsonObject();
            }
        }
        throw new IOException("judge-limits.json not found from " + Paths.get("").toAbsolutePath());
    }

    @Test
    void constantsMatchContract() throws IOException {
        JsonObject contract = readContract();
        JsonObject time = contract.getAsJsonObject("timeLimitMs");
        JsonObject memory = contract.getAsJsonObject("memoryLimitMb");

        assertEquals(time.get("default").getAsLong(), Constants.defaultTimeLimitMs);
        assertEquals(time.get("min").getAsLong(), Constants.minTimeLimitMs);
        assertEquals(time.get("max").getAsLong(), Constants.maxTimeLimitMs);
        assertEquals(memory.get("default").getAsLong(), Constants.defaultMemoryLimitMb);
        assertEquals(memory.get("min").getAsLong(), Constants.minMemoryLimitMb);
        assertEquals(memory.get("max").getAsLong(), Constants.maxMemoryLimitMb);
        assertEquals(contract.getAsJsonObject("testCases").get("max").getAsInt(), Constants.maxTestCases);
        assertEquals(contract.getAsJsonObject("codeLength").get("max").getAsInt(), Constants.maxCodeLength);
        assertEquals(contract.getAsJsonObject("outputBytes").get("max").getAsInt(), Constants.maxStdoutLength);
    }
}
