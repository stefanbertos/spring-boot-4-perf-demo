package com.example.perftester.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;

@Configuration
@EnableAsync
public class AsyncConfig {

    public static final String MQ_SENDER_EXECUTOR = "mqSenderExecutor";
    private static final int MQ_SENDER_THREAD_COUNT = 20;

    @Bean(MQ_SENDER_EXECUTOR)
    public ExecutorService mqSenderExecutor() {
        return Executors.newFixedThreadPool(MQ_SENDER_THREAD_COUNT);
    }
}
