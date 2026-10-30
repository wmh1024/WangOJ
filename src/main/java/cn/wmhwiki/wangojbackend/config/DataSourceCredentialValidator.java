package cn.wmhwiki.wangojbackend.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * 启动时校验数据库口令，拒绝空值、低于长度下限的值与公开占位值。
 *
 * <p>与 {@link PasswordSaltValidator} 同一范式：示例文件里的占位值一旦被原样带到
 * 生产环境，就是“能正常启动、但口令是公开常量”的静默风险。这里在启动阶段直接失败，
 * 并给出可操作的修复指引。</p>
 *
 * <p>只在确实需要连接数据库时校验：若 {@code app.datasource.validate-password}
 * 显式关闭（例如只想跑不碰库的单测或工具类），则跳过。</p>
 */
@Component
public class DataSourceCredentialValidator {

    /** 与 .env.example 的说明保持一致。 */
    static final int MIN_LENGTH = 12;

    static final Set<String> PLACEHOLDERS = new HashSet<>(Arrays.asList(
            "change-me",
            "change-me-please",
            "please-generate-a-random-32-chars-string",
            "__REQUIRED__"
    ));

    private final String password;

    private final boolean enabled;

    public DataSourceCredentialValidator(
            @Value("${app.datasource.validate-password:true}") boolean enabled,
            @Value("${spring.datasource.password:}") String password) {
        this.enabled = enabled;
        this.password = password == null ? "" : password.trim();
    }

    @PostConstruct
    public void validate() {
        if (!enabled) {
            return;
        }
        if (password.isEmpty()) {
            throw new IllegalStateException("DB_PASSWORD 未配置。请设置数据库口令（至少 " + MIN_LENGTH
                    + " 字符，例如 openssl rand -hex 32），并通过环境变量注入。");
        }
        if (PLACEHOLDERS.contains(password)) {
            throw new IllegalStateException("DB_PASSWORD 仍为示例占位值（" + password
                    + "），请替换为随机口令（例如 openssl rand -hex 32）。");
        }
        if (password.length() < MIN_LENGTH) {
            throw new IllegalStateException("DB_PASSWORD 长度不足 " + MIN_LENGTH
                    + " 个字符，请使用随机口令（例如 openssl rand -hex 32）。");
        }
    }
}
