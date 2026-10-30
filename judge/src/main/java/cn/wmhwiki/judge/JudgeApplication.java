/*
 * 本文件修改自 code-judge (https://github.com/kk-OJ/code-judge)。
 * Copyright (c) zyyzyykk，Apache License 2.0；许可证全文见 judge/LICENSE。
 */
package cn.wmhwiki.judge;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.EnableScheduling;

/**
 * 判题沙箱启动类。
 */
@SpringBootApplication
@EnableScheduling
public class JudgeApplication {

    private static final Logger log = LoggerFactory.getLogger(JudgeApplication.class);

    @Value("${judge.token:}")
    private String judgeToken;

    public static void main(String[] args) {
        SpringApplication.run(JudgeApplication.class, args);
    }

    /**
     * 启动后提示鉴权配置状态。
     *
     * <p>未配置 {@code JUDGE_TOKEN} 时所有判题请求都会被拒绝（fail-closed），
     * 这里只给出明确提示，不打印密钥本身。</p>
     */
    @EventListener(ApplicationReadyEvent.class)
    public void checkToken() {
        String token = judgeToken == null ? "" : judgeToken.trim();
        if (token.isEmpty() || "__REQUIRED__".equals(token)
                || "please-generate-a-random-token".equals(token)) {
            log.warn("未配置有效的 JUDGE_TOKEN（空值或示例占位值），所有判题请求将返回 503。"
                    + "请在后端与沙箱两端设置相同的随机密钥（例如 openssl rand -hex 32）。");
        } else {
            log.info("判题鉴权已启用（JUDGE_TOKEN 长度 {}）", token.length());
        }
    }
}
