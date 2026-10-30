package cn.wmhwiki.wangojbackend.util;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.DigestUtils;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.Base64;

/**
 * 密码哈希工具。
 *
 * <p>新密码使用 PBKDF2-HMAC-SHA256 + 随机盐，格式为：
 * {@code pbkdf2$<iterations>$<saltBase64>$<hashBase64>}。</p>
 *
 * <p>为了兼容历史数据，仍然可以校验旧的 {@code md5(password + salt)} 格式，
 * 校验成功后由调用方通过 {@link #needsUpgrade(String)} 判断并重新哈希。</p>
 *
 * @author WangOJ contributors
 */
@Slf4j
@Component
public class PasswordUtils {

    private static final String PBKDF2_PREFIX = "pbkdf2";
    private static final String ALGORITHM = "PBKDF2WithHmacSHA256";
    private static final int ITERATIONS = 120_000;
    private static final int SALT_BYTES = 16;
    private static final int KEY_BITS = 256;
    private static final int MD5_HEX_LENGTH = 32;

    /**
     * 允许的迭代次数区间。哈希参数存在数据库里，必须限定范围：
     * 太小等于削弱哈希强度，太大则一次登录就能长时间占用 CPU（可被利用做 DoS）。
     */
    private static final int MIN_ITERATIONS = 100_000;

    private static final int MAX_ITERATIONS = 1_000_000;

    private final SecureRandom secureRandom = new SecureRandom();
    private final String legacySalt;

    public PasswordUtils(@Value("${app.password-salt:}") String legacySalt) {
        this.legacySalt = StrUtil.nullToEmpty(legacySalt);
    }

    /**
     * 生成新的密码哈希。
     */
    public String encode(String rawPassword) {
        if (rawPassword == null) {
            throw new IllegalArgumentException("password must not be null");
        }
        byte[] salt = new byte[SALT_BYTES];
        secureRandom.nextBytes(salt);
        byte[] hash = pbkdf2(rawPassword.toCharArray(), salt, ITERATIONS);
        Base64.Encoder encoder = Base64.getEncoder().withoutPadding();
        return PBKDF2_PREFIX + "$" + ITERATIONS + "$"
                + encoder.encodeToString(salt) + "$"
                + encoder.encodeToString(hash);
    }

    /**
     * 校验密码。同时兼容历史 MD5 格式。
     */
    public boolean matches(String rawPassword, String storedPassword) {
        if (rawPassword == null || StrUtil.isBlank(storedPassword)) {
            return false;
        }
        if (storedPassword.startsWith(PBKDF2_PREFIX + "$")) {
            return matchesPbkdf2(rawPassword, storedPassword);
        }
        return matchesLegacyMd5(rawPassword, storedPassword);
    }

    /**
     * 该哈希是否需要用新算法重新生成。
     */
    public boolean needsUpgrade(String storedPassword) {
        return storedPassword == null || !storedPassword.startsWith(PBKDF2_PREFIX + "$");
    }

    private boolean matchesPbkdf2(String rawPassword, String storedPassword) {
        String[] parts = storedPassword.split("\\$");
        if (parts.length != 4) {
            return false;
        }
        try {
            int iterations = Integer.parseInt(parts[1]);
            if (iterations < MIN_ITERATIONS || iterations > MAX_ITERATIONS) {
                // 参数异常说明数据被篡改或迁移出错：拒绝校验并留痕
                log.warn("密码哈希迭代次数超出允许范围，拒绝校验: {}", iterations);
                return false;
            }
            byte[] salt = Base64.getDecoder().decode(parts[2]);
            byte[] expected = Base64.getDecoder().decode(parts[3]);
            // 空哈希或长度不符，直接失败，避免 PBKDF2 计算出无意义的长度
            if (expected.length == 0) {
                return false;
            }
            char[] chars = rawPassword.toCharArray();
            PBEKeySpec spec = new PBEKeySpec(chars, salt, iterations, KEY_BITS);
            byte[] actual;
            try {
                actual = SecretKeyFactory.getInstance(ALGORITHM).generateSecret(spec).getEncoded();
            } finally {
                // 及时清空临时密码副本，缩短其在堆中的存活时间
                spec.clearPassword();
                Arrays.fill(chars, (char) 0);
            }
            return MessageDigest.isEqual(expected, actual);
        } catch (IllegalArgumentException e) {
            return false;
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希校验失败", e);
        }
    }

    private boolean matchesLegacyMd5(String rawPassword, String storedPassword) {
        if (storedPassword.length() != MD5_HEX_LENGTH) {
            return false;
        }
        String md5 = DigestUtils.md5DigestAsHex(
                (rawPassword + legacySalt).getBytes(StandardCharsets.UTF_8));
        return MessageDigest.isEqual(
                md5.getBytes(StandardCharsets.UTF_8),
                storedPassword.toLowerCase().getBytes(StandardCharsets.UTF_8));
    }

    private byte[] pbkdf2(char[] password, byte[] salt, int iterations) {
        PBEKeySpec spec = new PBEKeySpec(password, salt, iterations, KEY_BITS);
        try {
            SecretKeyFactory factory = SecretKeyFactory.getInstance(ALGORITHM);
            return factory.generateSecret(spec).getEncoded();
        } catch (Exception e) {
            throw new IllegalStateException("密码哈希计算失败", e);
        } finally {
            spec.clearPassword();
        }
    }
}
