package com.example.rabbitmq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadTestService {

    private final MqttService mqttService;
    private final RabbitMqService rabbitMqService;
    private final KafkaService kafkaService;
    private final ObjectMapper objectMapper;

    // 테스트 활성화 여부 - 기본적으로 비활성화, application.yml에서 설정 가능
    @Value("${load.test.enabled}")
    private boolean enabled;

    @Value("${load.test.messages-per-batch:5000}")
    private int messagesPerBatch;
    
    @Value("${load.test.batch-interval:1000}")
    private int batchInterval;
    
    @Value("${load.test.duration-seconds}")
    private int testDurationSeconds;
    
    private LocalDateTime testStartTime;
    
    private AtomicInteger sentMessagesCount = new AtomicInteger(0);
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);

    @PostConstruct
    public void init() {
        if (enabled) {
            testStartTime = LocalDateTime.now();
            log.info("애플리케이션 시작 시 부하 테스트가 활성화되어 있으므로 testStartTime을 초기화합니다: {}", testStartTime);
        }
    }

    AtomicLong sendCount = new AtomicLong(0);

    @Scheduled(fixedRate = 200) // 0.2초 간격으로 실행
    public void sendBatchMessages() {
        if (!enabled) {
            return;
        }

        LocalDateTime start = LocalDateTime.now();

        // 테스트 시작 시간 설정
        if (testStartTime == null) {
            testStartTime = LocalDateTime.now();
            log.info("부하 테스트 시작: {}ms 간격으로 메시지를 MQTT에 전송합니다.", batchInterval);
        }

        if (LocalDateTime.now().isAfter(testStartTime.plusSeconds(testDurationSeconds))) {
            enabled = false;
            return;
        }

        for (int i = 0; i < 1000; i++) {
            try {
                String id = UUID.randomUUID().toString();

                Map<String, Object> row = new HashMap<>();
                row.put("id", id);
                Random random = new Random();
                for (int col = 1; col <= 199; col++) {
                    row.put("col" + col, random.nextDouble());
                }

                // MQTT 메시지 전송
                String message = objectMapper.writeValueAsString(row);
//                rabbitMqService.sendMessage(message);

                mqttService.sendMessage("test", message);
                sendCount.incrementAndGet();
            } catch (Exception e) {
                log.error("메시지 생성/전송 중 오류: {}", e.getMessage());
            }
        }

        Duration elapsed = Duration.between(start, LocalDateTime.now());
        log.info("메시지 전송 완료: 1000개, 실행 시간: {}ms", elapsed.toMillis());
    }
    
    public void setTestEnabled(boolean enabled) {
        this.enabled = enabled;
        if (enabled) {
            // 테스트 시작 시 카운터 초기화
            mqttService.reset();
            rabbitMqService.reset();
            kafkaService.reset();

            testStartTime = null;
            sentMessagesCount.set(0);
            log.info("부하 테스트가 활성화되었습니다. 다음 스케줄링 주기에 시작됩니다.");
        } else {
            log.info("부하 테스트가 비활성화되었습니다.");
        }
    }

    public boolean isTestEnabled() {
        return enabled;
    }

    public int getSentMessagesCount() {
        return sentMessagesCount.get();
    }

}