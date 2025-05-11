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
import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class LoadTestService {

    private final MqttService mqttService;
    private final ObjectMapper objectMapper;

    @Value("${load.test.enabled:false}")
    private boolean enabled;

    @Value("${load.test.messages-per-batch:1000}")
    private int messagesPerBatch;

    @Value("${load.test.batch-interval:200}")
    private int batchInterval;   // ms

    @Value("${load.test.duration-seconds:60}")
    private int testDurationSeconds;

    // 미리 만들어둘 메시지 풀
    private List<String> preGenerated;

    // 비동기 전송 전용 풀 (IO 바운드라면 코어×2~×4 정도)
    private final ExecutorService sendPool =
            Executors.newFixedThreadPool(Runtime.getRuntime().availableProcessors() * 3);

    private LocalDateTime startTime;
    private final AtomicLong sendCount = new AtomicLong();

    @PostConstruct
    public void init() throws Exception {
        if (enabled) {
            preGenerateMessages();
            log.info("▶ 메시지 풀 생성 완료 (batchSize={})", messagesPerBatch);
        }
    }

    private void preGenerateMessages() throws Exception {
        preGenerated = new ArrayList<>(messagesPerBatch);
        Random rnd = new Random();
        for (int i = 0; i < messagesPerBatch; i++) {
            Map<String,Object> row = new HashMap<>();
            row.put("id", UUID.randomUUID().toString());
            for (int c = 1; c <= 199; c++) {
                row.put("col"+c, rnd.nextDouble());
            }
            preGenerated.add(objectMapper.writeValueAsString(row));
        }
    }

    @Scheduled(fixedRateString = "${load.test.batch-interval}")
    public void sendBatchMessages() {
        if (!enabled) return;

        if (startTime == null) {
            startTime = LocalDateTime.now();
            log.info("▶ 부하 테스트 시작: duration={}초, batchSize={}", testDurationSeconds, messagesPerBatch);
        }

        // 종료 체크
        if (Duration.between(startTime, LocalDateTime.now()).getSeconds() >= testDurationSeconds) {
            enabled = false;
            long elapsed = Duration.between(startTime, LocalDateTime.now()).getSeconds();
            log.info("▶ 부하 테스트 종료: 실제경과={}초, 총전송={}건", elapsed, sendCount.get());
            return;
        }

        long batchStart = System.currentTimeMillis();
        // 비동기로만 던지고 리턴
        for (int i = 0; i < preGenerated.size(); i++) {
            final String payload = preGenerated.get(i);
            sendPool.execute(() -> {
                try {
                    mqttService.sendMessage("test", payload);
                    sendCount.incrementAndGet();
                } catch (Exception e) {
                    log.error("전송 실패", e);
                }
            });
        }
        long elapsed = System.currentTimeMillis() - batchStart;
        log.info("메시지 요청 던짐: {}개, scheduling 시간={}ms (누적 전송 요청={}건)",
                messagesPerBatch, elapsed, sendCount.get());
    }

    public void setTestEnabled(boolean on) throws Exception {
        this.enabled = on;
        if (on) {
            sendCount.set(0);
            startTime = null;
            preGenerateMessages();
            log.info("▶ 부하 테스트 켜짐 (재시작 준비)");
        } else {
            log.info("▶ 부하 테스트 꺼짐");
        }
    }

    public boolean isTestEnabled() {
        return enabled;
    }

    public long getSentMessagesCount() {
        return sendCount.get();
    }
}