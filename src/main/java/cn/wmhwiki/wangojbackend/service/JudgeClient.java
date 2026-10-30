package cn.wmhwiki.wangojbackend.service;

import cn.hutool.core.util.StrUtil;
import cn.wmhwiki.wangojbackend.pojo.TestCase;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * 判题沙箱客户端（协议 v1.0）。
 *
 * <p>职责边界：只负责"把代码和输入发出去、把执行结果收回来"，不做任何答案比较。
 * {@link HttpClient} 是共享单例（内部自带连接复用），避免每次提交都新建连接与 selector 线程。</p>
 */
@Slf4j
@Component
public class JudgeClient {

    /**
     * 判题协议版本
     */
    private static final String PROTOCOL_VERSION = "1.0";

    private static final String JUDGE_MODE = "judge";

    private static final String JUDGE_TOKEN_HEADER = "X-Judge-Token";

    /**
     * 沙箱返回的编译错误标识
     */
    private static final String VERDICT_COMPILE_ERROR = "COMPILE_ERROR";

    /**
     * 沙箱返回的执行完成标识
     */
    private static final String VERDICT_EXECUTED = "EXECUTED";

    /**
     * 编译时间限制（毫秒），必须与沙箱允许的上限一致
     */
    private static final long COMPILE_TIME_LIMIT_MS = 5000L;

    /**
     * 单个测试点返回的最大输出字符数
     */
    private static final long MAX_OUTPUT_BYTES = 64 * 1024;

    private static final int MAX_LOG_BODY = 256;

    private final HttpClient httpClient;

    private final Gson gson = new Gson();

    private final String judgeUrl;

    private final String judgeToken;

    private final long readTimeoutMs;

    public JudgeClient(@Value("${judge.url}") String judgeUrl,
                       @Value("${judge.token:}") String judgeToken,
                       @Value("${judge.connect-timeout-ms:2000}") long connectTimeoutMs,
                       @Value("${judge.read-timeout-ms:60000}") long readTimeoutMs) {
        this.judgeUrl = judgeUrl;
        this.judgeToken = judgeToken;
        this.readTimeoutMs = readTimeoutMs;
        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofMillis(connectTimeoutMs))
                .followRedirects(HttpClient.Redirect.NEVER)
                .build();
    }

    /**
     * 调用沙箱判题。
     *
     * @param requestId     判题请求 ID
     * @param language      语言标识（c/c++/java/python/javascript/golang）
     * @param code          源码
     * @param testCases     测试点（只下发输入，标准输出仅在后端使用）
     * @param timeLimitMs   单个测试点时间限制
     * @param memoryLimitMb 内存限制
     * @return 沙箱响应；失败时返回带错误信息的 {@link JudgeResult}
     */
    public JudgeResult judge(String requestId, String language, String code, List<TestCase> testCases,
                             long timeLimitMs, long memoryLimitMb) {
        if (StrUtil.isBlank(judgeToken)) {
            log.error("未配置 judge.token，拒绝判题, requestId={}", requestId);
            return JudgeResult.failed("未配置判题沙箱令牌");
        }
        String body = buildRequestJson(requestId, language, code, testCases, timeLimitMs, memoryLimitMb);
        HttpResponse<String> response = post(requestId, body);
        if (response == null) {
            return JudgeResult.failed("判题沙箱不可用");
        }
        return parseResponse(requestId, response.body());
    }

    /**
     * 构造协议 v1.0 请求体。
     *
     * <p><b>注意</b>：不发送 {@code expectedOutput}。沙箱只负责执行，标准答案不应离开后端。</p>
     */
    private String buildRequestJson(String requestId, String language, String code, List<TestCase> testCases,
                                    long timeLimitMs, long memoryLimitMb) {
        JsonObject limits = new JsonObject();
        limits.addProperty("timeLimitMs", timeLimitMs);
        limits.addProperty("memoryLimitMb", memoryLimitMb);
        limits.addProperty("compileTimeLimitMs", COMPILE_TIME_LIMIT_MS);
        limits.addProperty("maxOutputBytes", MAX_OUTPUT_BYTES);

        JsonArray cases = new JsonArray();
        for (TestCase testCase : testCases) {
            JsonObject item = new JsonObject();
            item.addProperty("input", testCase.getInput() == null ? "" : testCase.getInput());
            cases.add(item);
        }

        JsonObject body = new JsonObject();
        body.addProperty("requestId", requestId);
        body.addProperty("protocolVersion", PROTOCOL_VERSION);
        body.addProperty("language", language);
        body.addProperty("code", code);
        body.addProperty("mode", JUDGE_MODE);
        body.add("limits", limits);
        body.add("testCases", cases);
        return gson.toJson(body);
    }

    private HttpResponse<String> post(String requestId, String body) {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(judgeUrl))
                .timeout(Duration.ofMillis(readTimeoutMs))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header(JUDGE_TOKEN_HEADER, judgeToken)
                .POST(HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8))
                .build();

        log.info("发送判题请求, requestId={}", requestId);
        try {
            HttpResponse<String> response =
                    httpClient.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
            if (response.statusCode() == 401 || response.statusCode() == 503) {
                log.error("沙箱鉴权失败, requestId={}, status={}（请检查两端 JUDGE_TOKEN 是否一致）",
                        requestId, response.statusCode());
                return null;
            }
            if (response.statusCode() != 200) {
                log.error("沙箱 HTTP 状态异常, requestId={}, status={}, body={}",
                        requestId, response.statusCode(), truncate(response.body()));
                return null;
            }
            return response;
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("调用沙箱被中断, requestId={}", requestId);
            return null;
        } catch (Exception e) {
            log.error("调用沙箱失败, requestId={}, message={}", requestId, e.getMessage());
            return null;
        }
    }

    /**
     * 解析沙箱响应；结构不合法返回失败结果。
     */
    private JudgeResult parseResponse(String requestId, String body) {
        JsonObject result;
        try {
            JsonElement element = JsonParser.parseString(body);
            if (element == null || !element.isJsonObject()) {
                log.error("沙箱返回不是 JSON 对象, requestId={}", requestId);
                return JudgeResult.failed("判题响应格式错误");
            }
            result = element.getAsJsonObject();
        } catch (JsonSyntaxException e) {
            log.error("沙箱返回无法解析, requestId={}", requestId);
            return JudgeResult.failed("判题响应无法解析");
        }

        String responseRequestId = optString(result, "requestId");
        if (responseRequestId != null && !responseRequestId.equals(requestId)) {
            log.warn("沙箱返回的 requestId 与请求不一致, expected={}, actual={}", requestId, responseRequestId);
        }
        Integer code = optInt(result, "code");
        if (code == null || code != 200) {
            log.error("沙箱返回业务错误, requestId={}, code={}, message={}",
                    requestId, code, optString(result, "message"));
            return JudgeResult.failed("判题沙箱返回错误：" + optString(result, "message"));
        }
        String verdict = optString(result, "verdict");
        if (VERDICT_COMPILE_ERROR.equals(verdict)) {
            return JudgeResult.compileError(nullToEmpty(optString(result, "compileOutput")));
        }
        if (!VERDICT_EXECUTED.equals(verdict)) {
            log.error("沙箱未能完成执行, requestId={}, verdict={}, message={}",
                    requestId, verdict, optString(result, "message"));
            return JudgeResult.failed("沙箱未能完成执行");
        }
        return JudgeResult.executed(parseCases(result));
    }

    private List<CaseResult> parseCases(JsonObject result) {
        List<CaseResult> cases = new ArrayList<>();
        if (!result.has("cases") || !result.get("cases").isJsonArray()) {
            return cases;
        }
        for (JsonElement element : result.getAsJsonArray("cases")) {
            if (element == null || !element.isJsonObject()) {
                cases.add(null);
                continue;
            }
            JsonObject item = element.getAsJsonObject();
            CaseResult caseResult = new CaseResult();
            caseResult.setIndex(optInt(item, "index"));
            caseResult.setVerdict(optString(item, "verdict"));
            caseResult.setStdout(optString(item, "stdout"));
            caseResult.setStderr(optString(item, "stderr"));
            // 缺失时返回 null，交由比较层判断"未知"，而不是按 0 处理
            caseResult.setTimeMs(optLongOrNull(item, "timeMs"));
            caseResult.setMemoryBytes(optLongOrNull(item, "memoryBytes"));
            cases.add(caseResult);
        }
        return cases;
    }

    private static String optString(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsString();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Integer optInt(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsInt();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static Long optLongOrNull(JsonObject object, String key) {
        if (object == null || !object.has(key) || object.get(key).isJsonNull()) {
            return null;
        }
        try {
            return object.get(key).getAsLong();
        } catch (RuntimeException e) {
            return null;
        }
    }

    private static String nullToEmpty(String value) {
        return value == null ? "" : value;
    }

    /**
     * 日志中截断外部返回内容，避免把用户代码/输出写入日志。
     */
    private static String truncate(String text) {
        if (text == null) {
            return "";
        }
        return text.length() <= MAX_LOG_BODY ? text : text.substring(0, MAX_LOG_BODY) + "...";
    }

    /**
     * 沙箱判题结果。
     */
    @Getter
    public static final class JudgeResult {

        private final boolean executed;

        private final boolean compileError;

        private final String message;

        private final String compileOutput;

        private final List<CaseResult> cases;

        private JudgeResult(boolean executed, boolean compileError, String message,
                            String compileOutput, List<CaseResult> cases) {
            this.executed = executed;
            this.compileError = compileError;
            this.message = message;
            this.compileOutput = compileOutput;
            this.cases = cases;
        }

        static JudgeResult executed(List<CaseResult> cases) {
            return new JudgeResult(true, false, "judged", "", cases);
        }

        static JudgeResult compileError(String compileOutput) {
            return new JudgeResult(false, true, "compilation error", compileOutput, new ArrayList<>());
        }

        static JudgeResult failed(String message) {
            return new JudgeResult(false, false, message, "", new ArrayList<>());
        }
    }

    /**
     * 单个测试点执行结果。
     */
    @Getter
    public static final class CaseResult {

        private Integer index;

        private String verdict;

        private String stdout;

        private String stderr;

        private Long timeMs;

        private Long memoryBytes;

        void setIndex(Integer index) {
            this.index = index;
        }

        void setVerdict(String verdict) {
            this.verdict = verdict;
        }

        void setStdout(String stdout) {
            this.stdout = stdout;
        }

        void setStderr(String stderr) {
            this.stderr = stderr;
        }

        void setTimeMs(Long timeMs) {
            this.timeMs = timeMs;
        }

        void setMemoryBytes(Long memoryBytes) {
            this.memoryBytes = memoryBytes;
        }
    }
}
