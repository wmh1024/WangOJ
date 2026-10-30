package cn.wmhwiki.wangojbackend.util;

import org.junit.jupiter.api.Test;
import org.springframework.util.DigestUtils;

import java.nio.charset.StandardCharsets;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PasswordUtilsTest {

    private final PasswordUtils passwordUtils = new PasswordUtils("test-salt");

    @Test
    void encodeThenMatches() {
        String hash = passwordUtils.encode("123456");
        assertNotEquals("123456", hash);
        assertTrue(hash.startsWith("pbkdf2$"));
        assertTrue(passwordUtils.matches("123456", hash));
        assertFalse(passwordUtils.matches("1234567", hash));
        assertFalse(passwordUtils.needsUpgrade(hash));
    }

    @Test
    void randomSaltProducesDifferentHashes() {
        String first = passwordUtils.encode("same-password");
        String second = passwordUtils.encode("same-password");
        assertNotEquals(first, second);
        assertTrue(passwordUtils.matches("same-password", first));
        assertTrue(passwordUtils.matches("same-password", second));
    }

    @Test
    void legacyMd5StillMatchesAndNeedsUpgrade() {
        String legacy = DigestUtils.md5DigestAsHex("123456".getBytes(StandardCharsets.UTF_8));
        // 使用空盐构造，模拟历史 md5(password + SALT) 中 SALT 为空的情况
        PasswordUtils legacyUtils = new PasswordUtils("");
        // 历史实现是 md5(password + salt)，这里直接构造与之一致的值
        String md5 = DigestUtils.md5DigestAsHex(("123456" + "test-salt").getBytes(StandardCharsets.UTF_8));
        assertTrue(passwordUtils.matches("123456", md5));
        assertTrue(passwordUtils.needsUpgrade(md5));
        // 长度不符的字符串不应抛出异常
        assertFalse(legacyUtils.matches("123456", legacy.substring(0, 10)));
    }

    @Test
    void blankOrNullNeverMatches() {
        assertFalse(passwordUtils.matches(null, passwordUtils.encode("123456")));
        assertFalse(passwordUtils.matches("123456", null));
        assertFalse(passwordUtils.matches("123456", ""));
        assertFalse(passwordUtils.matches("123456", "not-a-hash"));
    }
}
