package cn.wmhwiki.wangojbackend.dto;

import cn.wmhwiki.wangojbackend.pojo.LimitOption;
import cn.wmhwiki.wangojbackend.pojo.TestCase;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * DTO 粗粒度校验单测（复杂密码规则仍由 PasswordPolicy 负责）。
 */
class DtoValidationTest {

    private final Validator validator = Validation.buildDefaultValidatorFactory().getValidator();

    @Test
    void loginUsesUnifiedMessageAndLengthLimit() {
        LoginDTO empty = new LoginDTO();
        Set<ConstraintViolation<LoginDTO>> violations = validator.validate(empty);

        assertEquals(2, violations.size());
        for (ConstraintViolation<LoginDTO> violation : violations) {
            assertEquals("用户名或密码错误", violation.getMessage());
        }

        LoginDTO tooLong = new LoginDTO("a", repeat('x', 65));
        assertEquals("用户名或密码错误", messages(tooLong).iterator().next());
    }

    @Test
    void registerDtoKeepsCoarseConstraintsOnly() {
        UserDTO dto = new UserDTO();
        dto.setUserAccount("a1");
        dto.setUserPassword("a");
        assertTrue(messages(dto).isEmpty(), "密码复杂度应由 PasswordPolicy 校验，而不是注解");

        dto.setUserAccount(repeat('a', 65));
        assertTrue(messages(dto).contains("用户名过长"));
    }

    @Test
    void commitlogRequiresProblemLanguageAndCode() {
        Set<String> messages = messages(new CommitlogDTO());

        assertTrue(messages.containsAll(Arrays.asList(
                "题目ID不能为空", "语言不能为空", "代码不能为空")));
    }

    @Test
    void aiCodeRequiresProblemAndCode() {
        Set<String> messages = messages(new AiCodeDTO());

        assertTrue(messages.containsAll(Arrays.asList(
                "题目ID不能为空", "代码不能为空")));
    }

    @Test
    void problemRequiresTitleContentAndTestCases() {
        ProblemDTO dto = ProblemDTO.builder()
                .title(" ")
                .content("")
                .testCase(Collections.<TestCase>emptyList())
                .build();

        Set<String> messages = messages(dto);
        assertTrue(messages.containsAll(Arrays.asList(
                "题目标题不能为空", "题目内容不能为空", "至少需要一个测试用例")));
    }

    @Test
    void problemRejectsTooManyTestCases() {
        List<TestCase> cases = new ArrayList<>();
        for (int i = 0; i < 11; i++) {
            cases.add(new TestCase("1", "1"));
        }
        ProblemDTO dto = ProblemDTO.builder()
                .title("A+B")
                .content("desc")
                .testCase(cases)
                .build();

        assertTrue(messages(dto).contains("测试用例不能超过 10 个"));
    }

    @Test
    void nestedTestCaseAndLimitOptionAreValidated() {
        List<TestCase> cases = Collections.singletonList(new TestCase(repeat('x', 64 * 1024 + 1), ""));
        ProblemDTO dto = ProblemDTO.builder()
                .title("A+B")
                .content("desc")
                .testCase(cases)
                .limitOption(new LimitOption(0L, -1L))
                .build();

        Set<String> messages = messages(dto);
        assertTrue(messages.contains("单个测试输入不能超过 65536 个字符"));
        assertTrue(messages.contains("时间限制不能小于 200ms"));
        assertTrue(messages.contains("内存限制不能小于 32MB"));
    }

    @Test
    void problemRejectsOutOfRangeLimits() {
        ProblemDTO dto = ProblemDTO.builder()
                .title("A+B")
                .content("desc")
                .testCase(Collections.singletonList(new TestCase("1 2\n", "3\n")))
                .limitOption(new LimitOption(999_999L, 999_999L))
                .build();

        Set<String> messages = messages(dto);
        assertTrue(messages.contains("时间限制不能超过 5000ms"));
        assertTrue(messages.contains("内存限制不能超过 512MB"));
    }

    @Test
    void problemRejectsOverlongTag() {
        ProblemDTO dto = ProblemDTO.builder()
                .title("A+B")
                .content("desc")
                .testCase(Collections.singletonList(new TestCase("1 2\n", "3\n")))
                .tags(Collections.singletonList(repeat('x', 33)))
                .build();

        assertTrue(messages(dto).contains("单个标签不能超过 32 个字符"));
    }

    @Test
    void passwordUpdateRequiresPasswordOnly() {
        PasswordUpdateDTO empty = new PasswordUpdateDTO();
        assertTrue(messages(empty).contains("密码不能为空"));

        PasswordUpdateDTO dto = new PasswordUpdateDTO();
        dto.setUserPassword("abc12345");
        assertTrue(messages(dto).isEmpty());
    }

    @Test
    void validProblemPasses() {
        ProblemDTO dto = ProblemDTO.builder()
                .title("A+B")
                .content("输入两个整数，输出和")
                .testCase(Collections.singletonList(new TestCase("1 2\n", "3\n")))
                .limitOption(new LimitOption(1000L, 256L))
                .build();

        assertFalse(messages(dto).iterator().hasNext());
    }

    private <T> Set<String> messages(T dto) {
        Set<ConstraintViolation<T>> violations = validator.validate(dto);
        Set<String> result = new java.util.HashSet<>();
        for (ConstraintViolation<T> violation : violations) {
            result.add(violation.getMessage());
        }
        return result;
    }

    private static String repeat(char c, int count) {
        char[] chars = new char[count];
        Arrays.fill(chars, c);
        return new String(chars);
    }
}
