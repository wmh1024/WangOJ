package cn.wmhwiki.wangojbackend.service.impl;

import cn.hutool.core.util.StrUtil;
import cn.wmhwiki.wangojbackend.config.AiProperties;
import cn.wmhwiki.wangojbackend.exception.BusinessException;
import cn.wmhwiki.wangojbackend.service.AIService;
import cn.wmhwiki.wangojbackend.service.ProblemService;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

/**
 * AI 代码解释服务。
 *
 * <p>直接调用 OpenAI 兼容的 {@code /v1/chat/completions} 接口，
 * 不依赖任何 snapshot 版本的 SDK，可通过 {@code APP_AI_ENABLED} 开关。</p>
 *
 * @author WangOJ contributors
 */
@Slf4j
@Service
public class AIServiceImpl implements AIService {

    private static final String NO_PROMPT_TEMPLATE = """
            # 角色
            你是一位专业的代码优化助手，擅长为给定的代码添加正确的注释（不写行尾注释），同时能对代码进行正确的格式化处理，且保证除格式化外不修改代码本身。

            ## 技能
            ### 技能 1: 处理代码任务
            1. 接收用户提供的代码相关问题（在“现在需要解决的问题”字段中）、题目描述（在“题目”字段中）以及现有代码（在“现在的代码”字段中）。
            2. 针对代码添加恰当的注释，按照通用的代码注释规范进行操作，注释应清晰明了，能准确反映代码逻辑。
            3. 对代码进行格式化，遵循代码所属语言的标准格式化规则，确保代码结构清晰、易读。

            ## 限制
            - 仅围绕代码注释添加和格式化任务进行回复，不处理与该任务无关的请求。
            - 除格式化外，严禁对原始代码进行任何实质性修改。
            - 输出内容应简洁明了，专注于完成代码注释和格式化的任务要求。

            ## 题目

            ```md
            %s
            ```

            ## 代码

            ```
            %s
            ```
            """;

    private static final String WITH_PROMPT_TEMPLATE = """
            # 角色
            你是一位ACM竞赛冠军，具备深厚的编程功底和丰富的竞赛经验。你要根据用户提出的问题，对用户提供的代码进行精准修改，同时能够清晰地解释修改思路和原因。

            ## 技能
            ### 技能 1: 分析问题与修改代码
            1. 仔细阅读用户提供的“现在需要解决的问题”描述。
            2. 认真研究“现在的代码”，找出代码中与解决问题相关的部分。
            3. 根据问题对代码进行修改，确保修改后的代码能够有效解决问题。
            4. 在给出修改后的代码时，用注释详细说明每一处修改的目的和思路。

            ### 技能 2: 讲解题目与代码逻辑
            1. 当用户有需求时，能够依据“题目”内容，清晰地讲解题目所涉及的算法、数据结构等关键知识点。
            2. 针对“现在的代码”，解释代码整体的逻辑架构，以及各个模块、函数的作用。

            ## 限制:
            - 只围绕用户提供的代码和要解决的问题进行讨论，拒绝回答与这些无关的话题。
            - 所输出的代码修改内容和讲解内容要条理清晰，逻辑连贯。
            - 代码注释应简洁明了，能够准确传达修改意图。

            ## 现在需要解决的问题

            %s

            ## 题目

            ```md
            %s
            ```

            ## 现在的代码

            ```
            %s
            ```
            """;

    /**
     * 全局共享 HttpClient（线程安全）。连接超时取配置的最大值，单请求超时由 HttpRequest 控制。
     */
    private static final long AI_CONNECT_TIMEOUT_MS = 10_000L;

    /**
     * 全局共享 HttpClient（线程安全，内部连接复用）。
     *
     * <p>连接超时取上限值，单次请求的整体超时由 {@code HttpRequest.timeout} 按配置控制，
     * 避免共享客户端把某次调用的短超时固化下来。</p>
     */
    private static final HttpClient HTTP_CLIENT = HttpClient.newBuilder()
            .connectTimeout(Duration.ofMillis(AI_CONNECT_TIMEOUT_MS))
            .build();

    @Resource
    private AiProperties aiProperties;

    @Resource
    private ProblemService problemService;

    @Override
    public String codeExplain(String code, Long problemId, String aiPrompt) {
        if (!aiProperties.isUsable()) {
            throw new BusinessException("本站未启用 AI 功能");
        }
        if (problemId == null) {
            throw new BusinessException("题目ID不能为空");
        }
        if (StrUtil.isBlank(code)) {
            throw new BusinessException("代码不能为空");
        }
        if (code.length() > aiProperties.getMaxCodeLength()) {
            throw new BusinessException("代码长度超过 AI 处理上限");
        }

        String problemContent = problemService.getProblemContent(problemId).getContent();
        if (problemContent == null) {
            problemContent = "";
        }
        String prompt = StrUtil.isBlank(aiPrompt)
                ? String.format(NO_PROMPT_TEMPLATE, problemContent, code)
                : String.format(WITH_PROMPT_TEMPLATE, aiPrompt, problemContent, code);

        return chat(prompt);
    }

    /**
     * 调用 OpenAI 兼容接口，返回第一条消息内容。
     */
    private String chat(String prompt) {
        JsonObject message = new JsonObject();
        message.addProperty("role", "user");
        message.addProperty("content", prompt);

        JsonArray messages = new JsonArray();
        messages.add(message);

        JsonObject body = new JsonObject();
        body.addProperty("model", aiProperties.getModel());
        body.add("messages", messages);
        body.addProperty("temperature", 0.3);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(aiProperties.chatCompletionsUrl()))
                .timeout(Duration.ofMillis(aiProperties.getReadTimeoutMs()))
                .header("Content-Type", "application/json; charset=UTF-8")
                .header("Accept", "application/json")
                .header("Authorization", "Bearer " + aiProperties.getApiKey().trim())
                .POST(HttpRequest.BodyPublishers.ofString(body.toString(), StandardCharsets.UTF_8))
                .build();

        long start = System.currentTimeMillis();
        HttpResponse<String> response;
        try {
            response = HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.error("AI 请求被中断");
            throw new BusinessException("AI 服务调用被中断");
        } catch (Exception e) {
            log.error("AI 请求失败: {}", e.getMessage());
            throw new BusinessException("AI 服务暂时不可用");
        }
        long cost = System.currentTimeMillis() - start;

        if (response.statusCode() != 200) {
            // 不打印响应体，避免把密钥/用户代码相关上下文写进日志
            log.error("AI 接口返回异常状态: status={}, cost={}ms", response.statusCode(), cost);
            throw new BusinessException("AI 服务返回错误（HTTP " + response.statusCode() + "）");
        }
        log.info("AI 请求完成, cost={}ms", cost);

        try {
            JsonElement root = JsonParser.parseString(response.body());
            JsonArray choices = root.getAsJsonObject().getAsJsonArray("choices");
            if (choices == null || choices.isEmpty()) {
                throw new BusinessException("AI 未返回内容");
            }
            JsonObject messageObject = choices.get(0).getAsJsonObject().getAsJsonObject("message");
            if (messageObject == null || !messageObject.has("content")) {
                throw new BusinessException("AI 返回内容格式异常");
            }
            return messageObject.get("content").getAsString();
        } catch (BusinessException e) {
            throw e;
        } catch (Exception e) {
            log.error("解析 AI 响应失败: {}", e.getMessage());
            throw new BusinessException("AI 返回内容解析失败");
        }
    }
}
