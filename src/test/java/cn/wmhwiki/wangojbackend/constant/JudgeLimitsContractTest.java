package cn.wmhwiki.wangojbackend.constant;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * 跨模块契约：后端边界常量必须与仓库根目录 judge-limits.json 一致。
 *
 * <p>沙箱侧有对应的 {@code JudgeLimitsContractTest}。修改协议边界时必须同时更新
 * JSON 与两端常量。</p>
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
        throw new IOException("judge-limits.json not found from working directory " + Paths.get("").toAbsolutePath());
    }

    @Test
    void timeAndMemoryBoundsMatchContract() throws IOException {
        JsonObject contract = readContract();
        JsonObject time = contract.getAsJsonObject("timeLimitMs");
        JsonObject memory = contract.getAsJsonObject("memoryLimitMb");

        assertEquals(time.get("default").getAsLong(), JudgeLimits.DEFAULT_TIME_LIMIT_MS);
        assertEquals(time.get("min").getAsLong(), JudgeLimits.MIN_TIME_LIMIT_MS);
        assertEquals(time.get("max").getAsLong(), JudgeLimits.MAX_TIME_LIMIT_MS);
        assertEquals(memory.get("default").getAsLong(), JudgeLimits.DEFAULT_MEMORY_LIMIT_MB);
        assertEquals(memory.get("min").getAsLong(), JudgeLimits.MIN_MEMORY_LIMIT_MB);
        assertEquals(memory.get("max").getAsLong(), JudgeLimits.MAX_MEMORY_LIMIT_MB);
        assertEquals(contract.getAsJsonObject("testCases").get("max").getAsInt(),
                JudgeLimits.MAX_TEST_CASES);
    }

    @Test
    void contractFileIsDocumented() {
        assertTrue(Files.exists(Paths.get("docs", "judge-protocol.md"))
                        || Files.exists(Paths.get("..", "docs", "judge-protocol.md")),
                "协议文档应存在于仓库中");
    }
}
