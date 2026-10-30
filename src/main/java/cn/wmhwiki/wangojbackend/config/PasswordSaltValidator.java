package cn.wmhwiki.wangojbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 启动时校验密码哈希盐，拒绝空值与公开占位值。
 *
 * <p>该盐只用于兼容历史 {@code md5(password + salt)} 数据。若使用公开默认值启动，
 * 所有历史 MD5 密码都等于使用公开常量做盐，等同于没有盐。因此这里在应用启动阶段
 * 直接失败，并给出可操作的修复指引，避免“能启动但事实上不安全”。</p>
 */
@Component
public class PasswordSaltValidator {

    private static final int MIN_LENGTH = 16;

    private static final Set<String> PLACEHOLDERS = new HashSet<>(Arrays.asList(
            "change-me-please",
            "please-generate-a-random-32-chars-string",
            "__REQUIRED__"
    ));

    private final String passwordSalt;

    public PasswordSaltValidator(@Value("${app.password-salt:}") String passwordSalt) {
        this.passwordSalt = passwordSalt == null ? "" : passwordSalt.trim();
    }

    @PostConstruct
    public void validate() {
        if (passwordSalt.isEmpty()) {
            throw new IllegalStateException("APP_PASSWORD_SALT 未配置。请生成随机值（例如 openssl rand -hex 32）"
                    + "并通过环境变量注入；该值上线后不可更改。");
        }
        if (PLACEHOLDERS.contains(passwordSalt)) {
            throw new IllegalStateException("APP_PASSWORD_SALT 仍为示例占位值，请替换为随机值（例如 openssl rand -hex 32）。");
        }
        if (passwordSalt.length() < MIN_LENGTH) {
            throw new IllegalStateException("APP_PASSWORD_SALT 长度不足 " + MIN_LENGTH + " 个字符，请使用随机长字符串。");
        }
    }
}
