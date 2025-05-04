package com.example.rabbitmq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    public void sendMessage(String topic, String message) {
        try {
            log.info("Sending message to Kafka topic '{}': {}", topic, message);
            kafkaTemplate.send(topic, message);
        } catch (Exception e) {
            log.error("Failed to send message to Kafka: {}", e.getMessage(), e);
        }
    }

    @KafkaListener(topics = "${spring.kafka.topic}", groupId = "${spring.kafka.consumer.group-id}")
    public void consumeMessage(String message) {
        log.info("Received message from Kafka: {}", message);

        processMessage(message);
    }

    private void processMessage(String message) {
        log.info("Processing message: {}", message);
    }

}
