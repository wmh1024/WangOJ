package cn.wmhwiki.wangojbackend.service;

import cn.wmhwiki.wangojbackend.constant.JudgeStatus;
import cn.wmhwiki.wangojbackend.domain.Commitlog;
import cn.wmhwiki.wangojbackend.domain.Problem;
import cn.wmhwiki.wangojbackend.domain.User;
import cn.wmhwiki.wangojbackend.dto.CommitlogDTO;
import cn.wmhwiki.wangojbackend.mapper.CommitlogMapper;
import cn.wmhwiki.wangojbackend.mapper.ProblemMapper;
import cn.wmhwiki.wangojbackend.mapper.UserMapper;
import cn.wmhwiki.wangojbackend.util.UserHolder;
import cn.wmhwiki.wangojbackend.vo.UserVO;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.mockito.stubbing.Answer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.MySQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import java.util.Collections;
import java.util.Date;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

/**
 * 判题主链路集成测试：提交 → Waiting → 执行 → 最终状态落库。
 *
 * <p>默认不运行（Surefire 排除 {@code it} 标签）。需要 Docker，执行：
 * {@code mvn -Prun-it -Dtest=JudgeFlowIntegrationTest test}。</p>
 */
@Tag("it")
@Testcontainers(disabledWithoutDocker = true)
@SpringBootTest(properties = {
        "app.password-salt=integration-test-salt-0123456789",
        "spring.main.allow-bean-definition-overriding=true"
})
class JudgeFlowIntegrationTest {

    @Container
    static final MySQLContainer<?> MYSQL = new MySQLContainer<>("mysql:8.0")
            .withDatabaseName("wangoj")
            .withUsername("wangoj")
            // 需满足 DataSourceCredentialValidator 的 12 字符下限，否则应用拒绝启动
            .withPassword("wangoj-test-pass")
            .withInitScript("db/schema.sql");

    @Container
    static final GenericContainer<?> REDIS = new GenericContainer<>("redis:7-alpine")
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", MYSQL::getJdbcUrl);
        registry.add("spring.datasource.username", MYSQL::getUsername);
        registry.add("spring.datasource.password", MYSQL::getPassword);
        registry.add("spring.data.redis.host", REDIS::getHost);
        registry.add("spring.data.redis.port", () -> REDIS.getMappedPort(6379));
        registry.add("spring.data.redis.password", () -> "");
        registry.add("judge.token", () -> "integration-test-token");
        registry.add("knife4j.enable", () -> "false");
    }

    @Autowired
    private CommitlogService commitlogService;

    @Autowired
    private CommitlogMapper commitlogMapper;

    @Autowired
    private ProblemMapper problemMapper;

    @Autowired
    private UserMapper userMapper;

    @MockBean
    private JudgeClient judgeClient;

    @MockBean
    private ThreadPoolTaskExecutor taskExecutor;

    @AfterEach
    void clearUser() {
        UserHolder.removeUserVO();
    }

    @Test
    void commitTransitionsFromWaitingToFinalStatus() {
        doAnswer((Answer<Void>) invocation -> {
            Runnable task = invocation.getArgument(0);
            task.run();
            return null;
        }).when(taskExecutor).execute(any(Runnable.class));

        JudgeClient.CaseResult caseResult = new JudgeClient.CaseResult();
        caseResult.setIndex(0);
        caseResult.setVerdict("ACCEPTED");
        caseResult.setStdout("3\n");
        caseResult.setStderr("");
        caseResult.setTimeMs(12L);
        caseResult.setMemoryBytes(2048L);
        when(judgeClient.judge(anyString(), anyString(), anyString(), anyList(), anyLong(), anyLong()))
                .thenReturn(JudgeClient.JudgeResult.executed(Collections.singletonList(caseResult)));

        User user = new User();
        user.setUserAccount("it-user-" + System.nanoTime());
        user.setUserPassword("pbkdf2$120000$c2FsdA$aGFzaA");
        user.setUserRole("user");
        user.setCreatedTime(new Date());
        user.setIsDeleted(0);
        userMapper.insert(user);

        Problem problem = new Problem();
        problem.setTitle("A+B");
        problem.setContent("给定两个整数，输出和");
        problem.setTags("[]");
        problem.setTestCase("[{\"input\":\"1 2\n\",\"output\":\"3\n\"}]");
        problem.setLimitOption("{\"timeoutLimit\":1000,\"memoryLimit\":256}");
        problem.setCommitNum(0);
        problem.setAcceptNum(0);
        problem.setCreatedTime(new Date());
        problem.setIsDeleted(0);
        problemMapper.insert(problem);

        UserVO userVO = new UserVO();
        userVO.setId(user.getId());
        UserHolder.saveUserVO(userVO);

        CommitlogDTO dto = new CommitlogDTO();
        dto.setProblemId(problem.getId());
        dto.setLang("c++");
        dto.setCode("int main() { return 0; }");
        assertEquals("ok", commitlogService.commitCode(dto));

        Commitlog saved = commitlogMapper.selectOne(new LambdaQueryWrapper<Commitlog>()
                .eq(Commitlog::getProblemId, problem.getId())
                .orderByDesc(Commitlog::getId)
                .last("LIMIT 1"));
        assertNotNull(saved);
        assertEquals(JudgeStatus.ACCEPT, saved.getStatus());
        assertNotEquals(JudgeStatus.WAITING, saved.getStatus());

        Problem updated = problemMapper.selectById(problem.getId());
        assertEquals(1, updated.getAcceptNum());
        assertEquals(1, updated.getCommitNum());
    }
}
