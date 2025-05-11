package com.example.rabbitmq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final MetricsService metricsService;

    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    @KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMessage(String message) {
        long count = receivedCount.incrementAndGet();
        long bytes = receivedBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);

        if (count % 1000 == 0) {
            log.info("[Kafka] 누적 수신: {}건, {} bytes", count, bytes);
        }
    }

    @Scheduled(fixedRate = 5000)
    public void report() {
        log.info("[Kafka][5s] 수신 총계: {}건, {} bytes",
                receivedCount.get(), receivedBytes.get());
    }

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public long getReceivedBytes() {
        return receivedBytes.get();
    }

    public void reset() {
        receivedCount.set(0);
        receivedBytes.set(0);
    }
}