package com.example.rabbitmq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    @KafkaListener(topics = "${spring.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMessage(String message, Acknowledgment acknowledgment) {
        try {
            receivedCount.incrementAndGet();
            receivedBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);

            // Process message here

            // Acknowledge the message after successful processing
            acknowledgment.acknowledge();
        } catch (Exception e) {
            log.error("Error processing message: {}", message, e);
            // In case of error, you can choose not to acknowledge
            // which will result in redelivery based on your retry configuration
        }
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