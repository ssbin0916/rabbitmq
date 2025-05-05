package com.example.rabbitmq.service;

import com.example.rabbitmq.metrics.Metrics;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

@Service
@RequiredArgsConstructor
@Slf4j
public class LoadTestService {

    private final RabbitMqService rabbitMqService;
    private final ObjectMapper objectMapper;

    // 테스트 활성화 여부 - 기본적으로 비활성화, application.yml에서 설정 가능
    @Value("${load.test.enabled}")
    private boolean enabled;

    private final Metrics metrics = new Metrics();

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

        // 테스트 종료 시간 체크
        if (LocalDateTime.now().isAfter(testStartTime.plusSeconds(testDurationSeconds))) {
            enabled = false;
            return;
        }

        // 메시지 생성 및 전송
        for (int i = 0; i < 1000; i++) { // 0.2초마다 1000개의 메시지를 전송
            try {
                Map<String, Object> data = new HashMap<>();

                // 고유 ID를 생성
                String id = UUID.randomUUID().toString(); // 고유 ID

                // 199개의 난수 double 값을 생성
                Map<String, Object> row = new HashMap<>();
                row.put("id", id);
                Random random = new Random();
                for (int col = 1; col <= 199; col++) {
                    row.put("col" + col, random.nextDouble()); // 난수 double 값
                }

                // MQTT 메시지 전송
                String message = objectMapper.writeValueAsString(row);
                long rabbitStart = System.nanoTime();
                rabbitMqService.sendMessage(message);
                metrics.getRabbitMqSentCount().incrementAndGet();
                metrics.getRabbitMqSendTime().addAndGet(System.nanoTime() - rabbitStart);
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

    /**
     * 메트릭 상태 확인
     */
    public Metrics getMetrics() {
        return metrics;
    }

}