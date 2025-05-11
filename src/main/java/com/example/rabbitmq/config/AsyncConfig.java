package com.example.rabbitmq.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

import java.util.concurrent.ThreadPoolExecutor;

@EnableAsync
@Configuration
public class AsyncConfig {

    @Bean("mqttExecutor")
    public ThreadPoolTaskExecutor mqttExecutor() {
        ThreadPoolTaskExecutor exec = new ThreadPoolTaskExecutor();
        exec.setCorePoolSize(20);
        exec.setMaxPoolSize(50);
        // 충분히 큰 큐 용량을 잡아줍니다.
        exec.setQueueCapacity(20_000);
        exec.setThreadNamePrefix("mqtt-send-");
        // 거부 정책: 큐가 가득 차면 호출자 스레드가 직접 실행하게 함
        exec.setRejectedExecutionHandler(new ThreadPoolExecutor.CallerRunsPolicy());
        exec.initialize();
        return exec;
    }
}
