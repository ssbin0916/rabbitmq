package com.example.rabbitmq.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Random;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.OperatingSystemMXBean;
import java.lang.management.ThreadMXBean;
import java.io.File;


@Service
@RequiredArgsConstructor
@Slf4j
public class LoadTestService {

    private final RabbitMqService rabbitMqService;
    private final ObjectMapper objectMapper;

    // 테스트 활성화 여부 - 기본적으로 비활성화, application.yml에서 설정 가능
    @Value("${load.test.enabled:false}")
    private boolean enabled;
    
    // 스케줄링 주기마다 보낼 메시지 수
    @Value("${load.test.messages-per-batch:5000}")
    private int messagesPerBatch;
    
    // 배치 간격 (밀리초) - 기본 1초
    @Value("${load.test.batch-interval:1000}")
    private int batchInterval;
    
    // 테스트 시간 제한 (초) - 기본 10초
    @Value("${load.test.duration-seconds:10}")
    private int testDurationSeconds;
    
    // 테스트 시작 시간 (첫 메시지 전송 시 설정)
    private LocalDateTime testStartTime;
    
    // 보낸 메시지 수
    private AtomicInteger sentMessagesCount = new AtomicInteger(0);
    
    // 스레드 풀 - 메시지 전송 병렬화
    private final ExecutorService threadPool = Executors.newFixedThreadPool(10);
    
    /**
     * 설정된 간격(기본 1초)마다 실행되어 대량의 메시지를 RabbitMQ로 전송
     * 설정된 시간(기본 10초) 동안만 실행
     */
    @Scheduled(fixedRateString = "${load.test.batch-interval:1000}")
    public void sendBatchMessages() {
        // 테스트가 비활성화되어 있으면 실행하지 않음
        if (!enabled) {
            return;
        }
        
        // 테스트 시작 시간 설정 (첫 실행 시)
        if (testStartTime == null) {
            testStartTime = LocalDateTime.now();
            log.info("부하 테스트 시작: {}초 동안 {}ms마다 {}개 메시지 전송", 
                    testDurationSeconds, batchInterval, messagesPerBatch);
        }
        
        // 테스트 시간 제한 체크
        if (LocalDateTime.now().isAfter(testStartTime.plusSeconds(testDurationSeconds))) {
            // 테스트 종료 시간이 지나면 결과 출력하고 종료
            if (enabled) {
                int total = sentMessagesCount.get();
                double messagesPerSecond = (double) total / testDurationSeconds;
                log.info("부하 테스트 완료: {}초 동안 총 {}개 메시지 전송 (평균 {}/초)", 
                        testDurationSeconds, total, String.format("%.2f", messagesPerSecond));
                
                // 테스트 비활성화
                enabled = false;
                testStartTime = null;
                sentMessagesCount.set(0);
            }
            return;
        }
        
        // 현재 배치에서 전송할 메시지 수
        final int batchSize = messagesPerBatch;
        log.info("메시지 배치 전송 시작: {}개", batchSize);
        
        // 메시지 전송 시작 시간
        long batchStartTime = System.currentTimeMillis();
        
        // 병렬로 메시지 전송
        for (int i = 0; i < batchSize; i++) {
            final int messageId = sentMessagesCount.incrementAndGet();
            threadPool.submit(() -> {
                try {
                    rabbitMqService.sendMessage(objectMapper.writeValueAsString(new Random().nextInt()));
                } catch (Exception e) {
                    log.error("메시지 전송 실패 (ID: {}): {}", messageId, e.getMessage());
                    sentMessagesCount.decrementAndGet();
                }
            });
        }
        
        // 전송 소요 시간 측정
        long batchDuration = System.currentTimeMillis() - batchStartTime;
        log.info("메시지 배치 전송 완료: {}개, 소요시간: {}ms", batchSize, batchDuration);
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
    
    /**
     * 현재 테스트 상태 조회
     */
    public boolean isTestEnabled() {
        return enabled;
    }
    
    /**
     * 지금까지 보낸 메시지 수 조회
     */
    public int getSentMessagesCount() {
        return sentMessagesCount.get();
    }

    @Scheduled(fixedRateString = "${load.test.resource-check-interval:5000}")
    public void monitorSystemResources() {
        // 테스트 중이 아니라면 리소스 모니터링을 수행하지 않음
        if (!enabled && testStartTime == null) {
            return;
        }

        // JVM 메모리 사용량
        MemoryMXBean memoryBean = ManagementFactory.getMemoryMXBean();
        long usedHeapMemory = memoryBean.getHeapMemoryUsage().getUsed() / (1024 * 1024);
        long maxHeapMemory = memoryBean.getHeapMemoryUsage().getMax() / (1024 * 1024);
        long usedNonHeapMemory = memoryBean.getNonHeapMemoryUsage().getUsed() / (1024 * 1024);

        // CPU 사용량
        OperatingSystemMXBean osBean = ManagementFactory.getOperatingSystemMXBean();
        double systemLoadAverage = osBean.getSystemLoadAverage();
        double cpuLoad = -1;

        // CPU 사용률을 얻기 위한 추가 확인
        try {
            // 다양한 JVM 구현에서의 CPU 사용률 확인 시도
            // 1. com.sun.management 구현 시도
            if (osBean instanceof com.sun.management.OperatingSystemMXBean) {
                cpuLoad = ((com.sun.management.OperatingSystemMXBean) osBean).getProcessCpuLoad() * 100;
            }
            // 2. 다른 구현체 시도 (com.sun.management 클래스를 직접 참조할 수 없는 경우)
            else {
                Class<?> sunOsClass = Class.forName("com.sun.management.OperatingSystemMXBean");
                if (sunOsClass.isInstance(osBean)) {
                    java.lang.reflect.Method method = sunOsClass.getMethod("getProcessCpuLoad");
                    cpuLoad = ((Double) method.invoke(osBean)) * 100;
                }
            }
        } catch (Exception e) {
            log.warn("CPU 사용률을 가져올 수 없습니다: {}", e.getMessage());
        }

        // 스레드 정보
        ThreadMXBean threadBean = ManagementFactory.getThreadMXBean();
        int threadCount = threadBean.getThreadCount();
        int peakThreadCount = threadBean.getPeakThreadCount();
        long totalStartedThreadCount = threadBean.getTotalStartedThreadCount();

        // 디스크 공간
        File file = new File(".");
        long totalSpace = file.getTotalSpace() / (1024 * 1024 * 1024);
        long freeSpace = file.getFreeSpace() / (1024 * 1024 * 1024);
        long usableSpace = file.getUsableSpace() / (1024 * 1024 * 1024);

        // 리소스 정보 로깅
        StringBuilder sb = new StringBuilder();
        sb.append("\n=== 시스템 리소스 모니터링 ===\n");

        // 메모리 정보
        sb.append(String.format("힙 메모리: %dMB / %dMB (%.1f%%)\n",
                usedHeapMemory,
                maxHeapMemory,
                maxHeapMemory > 0 ? (double) usedHeapMemory / maxHeapMemory * 100 : 0));

        sb.append(String.format("비힙 메모리: %dMB\n", usedNonHeapMemory));

        // CPU 정보
        if (cpuLoad >= 0) {
            sb.append(String.format("CPU 사용률: %.2f%%\n", cpuLoad));
        } else {
            sb.append("CPU 사용률: 측정 불가\n");
        }

        if (systemLoadAverage >= 0) {
            sb.append(String.format("시스템 평균 부하: %.2f\n", systemLoadAverage));
        }

        // 스레드 정보
        sb.append(String.format("활성 스레드: %d (최대: %d, 총 시작: %d)\n",
                threadCount, peakThreadCount, totalStartedThreadCount));

        // 디스크 정보
        sb.append(String.format("디스크 공간: %dGB 사용 가능 / %dGB 전체 (사용 가능: %dGB)\n",
                freeSpace, totalSpace, usableSpace));

        // 메시지 전송 정보
        sb.append(String.format("전송된 메시지: %d\n", sentMessagesCount.get()));

        // 메시지 처리 속도 계산 (테스트 중인 경우)
        if (testStartTime != null) {
            long secondsSinceStart = java.time.Duration.between(testStartTime, LocalDateTime.now()).getSeconds();
            if (secondsSinceStart > 0) {
                double messagesPerSecond = (double) sentMessagesCount.get() / secondsSinceStart;
                sb.append(String.format("평균 메시지 전송 속도: %.2f/초\n", messagesPerSecond));
            }
        }

        sb.append("================================");

        log.info(sb.toString());
    }

}