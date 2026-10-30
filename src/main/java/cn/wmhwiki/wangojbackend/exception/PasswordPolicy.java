package cn.wmhwiki.wangojbackend.exception;

import cn.hutool.core.util.StrUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.regex.Pattern;

/**
 * 账号与密码策略。
 *
 * <p>注册、改密、登录三条路径共用同一套规则，避免"登录允许 128 位、注册只允许 64 位"
 * 这类规则漂移。</p>
 */
@Slf4j
@Component
public class PasswordPolicy {

    /**
     * 账号最大长度（与 DDL 中 {@code userAccount VARCHAR(64)} 一致）
     */
    public static final int MAX_ACCOUNT_LENGTH = 64;

    /**
     * 密码最小/最大长度
     */
    public static final int MIN_PASSWORD_LENGTH = 8;

    public static final int MAX_PASSWORD_LENGTH = 64;

    /**
     * 账号：字母开头，允许字母数字下划线，1-64 位
     */
    private static final Pattern ACCOUNT_PATTERN = Pattern.compile("^[A-Za-z]\\w{0,63}$");

    /**
     * 密码：8-64 位，必须同时包含字母与数字，允许常见特殊字符
     */
    private static final Pattern PASSWORD_PATTERN =
            Pattern.compile("^(?=.*[A-Za-z])(?=.*\\d)[A-Za-z\\d~!@#$%^&*_.-]{8,64}$");

    /**
     * 校验账号格式。
     *
     * @throws BusinessException 账号为空或不符合规则
     */
    public void validateAccount(String account) {
        if (StrUtil.isBlank(account)) {
            throw new BusinessException("用户名不能为空");
        }
        if (account.length() > MAX_ACCOUNT_LENGTH) {
            throw new BusinessException("用户名过长");
        }
        if (!ACCOUNT_PATTERN.matcher(account).matches()) {
            throw new BusinessException("用户名只能包含字母、数字、下划线，且需以字母开头");
        }
    }

    /**
     * 校验密码强度。
     *
     * @throws BusinessException 密码为空、长度不符或不满足复杂度
     */
    public void validatePassword(String password) {
        if (StrUtil.isBlank(password)) {
            throw new BusinessException("密码不能为空");
        }
        if (password.length() < MIN_PASSWORD_LENGTH || password.length() > MAX_PASSWORD_LENGTH) {
            throw new BusinessException("密码长度需在 " + MIN_PASSWORD_LENGTH + "-" + MAX_PASSWORD_LENGTH + " 位之间");
        }
        if (!PASSWORD_PATTERN.matcher(password).matches()) {
            throw new BusinessException("密码需同时包含字母和数字");
        }
    }
}
