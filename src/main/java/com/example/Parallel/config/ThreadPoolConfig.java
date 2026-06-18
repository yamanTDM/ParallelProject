package com.example.Parallel.config;

import org.slf4j.MDC;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.task.TaskDecorator;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@EnableScheduling
@Configuration
public class ThreadPoolConfig {

    @Bean
    public TaskDecorator mdcTaskDecorator() {
        return runnable -> {
            Map<String, String> contextMap = MDC.getCopyOfContextMap();

            return () -> {
                if (contextMap != null) {
                    MDC.setContextMap(contextMap);
                }

                try {
                    runnable.run();
                } finally {
                    MDC.clear();
                }
            };
        };
    }

    @Bean(name = "orderExecutor")
    public ThreadPoolTaskExecutor orderExecutor(
            TaskDecorator mdcTaskDecorator) {

        int cores = Runtime.getRuntime().availableProcessors();
        int poolSize = cores * 11;

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(50);
        executor.setRejectedExecutionHandler(new ThreadPoolExecutor.AbortPolicy());
        executor.setThreadNamePrefix("order-worker-");

        executor.setTaskDecorator(mdcTaskDecorator);

        executor.initialize();

        return executor;
    }
}