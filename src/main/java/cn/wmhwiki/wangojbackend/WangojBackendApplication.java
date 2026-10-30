package cn.wmhwiki.wangojbackend;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * 启动类
 *
 * @author WangOJ contributors
 */
@SpringBootApplication
@MapperScan("cn.wmhwiki.wangojbackend.mapper")
@EnableAsync
public class WangojBackendApplication {

    public static void main(String[] args) {
        SpringApplication.run(WangojBackendApplication.class, args);
    }

    /**
     * 判题线程池。
     *
     * <p>队列满时使用 CallerRuns 会阻塞 Web 线程，Abort 会让提交卡在 Waiting，
     * 因此这里显式抛出自定义异常，由 {@code CommitlogServiceImpl#commitCode} 捕获后
     * 把刚插入的提交标记为 System Error，并向上返回 503。</p>
     */
    @Bean
    public ThreadPoolTaskExecutor taskExecutor() {
        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(10);
        executor.setMaxPoolSize(20);
        executor.setQueueCapacity(100);
        executor.setThreadNamePrefix("Judge-");
        executor.setWaitForTasksToCompleteOnShutdown(true);
        executor.setAwaitTerminationSeconds(30);
        executor.setRejectedExecutionHandler((task, pool) -> {
            throw new JudgeQueueFullException("判题队列已满");
        });
        executor.initialize();
        return executor;
    }

    /**
     * 判题队列已满。
     */
    public static class JudgeQueueFullException extends RuntimeException {
        public JudgeQueueFullException(String message) {
            super(message);
        }
    }
}
