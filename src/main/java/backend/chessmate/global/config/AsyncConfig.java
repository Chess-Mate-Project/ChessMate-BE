package backend.chessmate.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskExecutor;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

import java.util.concurrent.Executor;
import java.util.concurrent.ThreadPoolExecutor;

@Configuration
@EnableAsync
public class AsyncConfig {

    @Bean(name = "InitStreaks")
    public Executor taskExcutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(10);
        ex.setMaxPoolSize(20);
        ex.setQueueCapacity(500);
        ex.setThreadNamePrefix("InitStreaks-");
        ex.setWaitForTasksToCompleteOnShutdown(true);
        ex.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        ex.setAwaitTerminationSeconds(180);
        ex.initialize();
        return ex;
    }

    @Bean(name = "defaultTask")
    public Executor taskExecutor() {
        ThreadPoolTaskExecutor ex = new ThreadPoolTaskExecutor();
        ex.setCorePoolSize(20);
        ex.setMaxPoolSize(50);
        ex.setQueueCapacity(2000);
        ex.setThreadNamePrefix("defaultTask-");
        ex.initialize();
        return ex;
    }

    @Bean // @EnableScheduling 이 켜져 있으면 Spring 이 이 Bean 을 스케줄 트리거 풀로 사용
    public ThreadPoolTaskScheduler taskScheduler() {
        ThreadPoolTaskScheduler sch = new ThreadPoolTaskScheduler();

        sch.setPoolSize(4);                         // 동시에 띄울 수 있는 @Scheduled 트리거 수
        sch.setThreadNamePrefix("sched-");          // 스케줄러 스레드 이름 접두사
        sch.setWaitForTasksToCompleteOnShutdown(true); // 종료 시 트리거 작업 마무리 대기
        sch.setAwaitTerminationSeconds(30);            // 트리거는 보통 짧으니 30초면 충분

        sch.initialize(); // 스케줄러 풀 초기화
        return sch;
    }

}
