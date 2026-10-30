package cn.wmhwiki.wangojbackend.exception;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * 账号/密码策略测试：登录、注册、改密必须共用同一套规则。
 */
class PasswordPolicyTest {

    private final PasswordPolicy policy = new PasswordPolicy();

    @Test
    void acceptsValidAccountAndPassword() {
        assertDoesNotThrow(() -> policy.validateAccount("alice_01"));
        assertDoesNotThrow(() -> policy.validatePassword("abcd1234"));
        assertDoesNotThrow(() -> policy.validatePassword("A1~!@#$%"));
    }

    @Test
    void rejectsInvalidAccount() {
        assertThrows(BusinessException.class, () -> policy.validateAccount(null));
        assertThrows(BusinessException.class, () -> policy.validateAccount(""));
        assertThrows(BusinessException.class, () -> policy.validateAccount("1abc"));
        assertThrows(BusinessException.class, () -> policy.validateAccount("a-b"));
        assertThrows(BusinessException.class, () -> policy.validateAccount("a".repeat(65)));
    }

    @Test
    void rejectsWeakPassword() {
        assertThrows(BusinessException.class, () -> policy.validatePassword(null));
        assertThrows(BusinessException.class, () -> policy.validatePassword("abc123"));
        assertThrows(BusinessException.class, () -> policy.validatePassword("abcdefgh"));
        assertThrows(BusinessException.class, () -> policy.validatePassword("12345678"));
        assertThrows(BusinessException.class, () -> policy.validatePassword("a1".repeat(33)));
        assertThrows(BusinessException.class, () -> policy.validatePassword("abc 1234"));
    }

    @Test
    void passwordLimitMatchesRegistration() {
        // 注册上限 64 位：登录侧也必须按同一上限拒绝更长的输入（原实现登录允许 128 位）
        assertEquals(64, PasswordPolicy.MAX_PASSWORD_LENGTH);
        assertEquals(64, PasswordPolicy.MAX_ACCOUNT_LENGTH);
    }
}
