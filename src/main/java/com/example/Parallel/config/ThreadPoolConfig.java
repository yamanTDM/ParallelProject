package com.example.Parallel.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@EnableAsync
@EnableScheduling
@Configuration
public class ThreadPoolConfig {


    @Bean(name = "orderExecutor")
    public ThreadPoolTaskExecutor orderExecutor() {
        int cores = Runtime.getRuntime().availableProcessors();


        int poolSize = cores * (1 + 10);

        ThreadPoolTaskExecutor executor = new ThreadPoolTaskExecutor();
        executor.setCorePoolSize(poolSize);
        executor.setMaxPoolSize(poolSize * 2);
        executor.setQueueCapacity(50);
        executor.setThreadNamePrefix("order-worker-");
        executor.initialize();

        return executor;
    }
}