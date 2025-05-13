package com.example.rabbitmq.service;

import com.example.rabbitmq.entity.KafkaMessageEntity;
import com.example.rabbitmq.repository.KafkaMessageRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.kafka.support.Acknowledgment;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class KafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final KafkaMessageRepository messageRepository;


    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();

    public void sendMessage(String topic, String message) {
        kafkaTemplate.send(topic, message);
    }

    @KafkaListener(topics = "${spring.kafka.topic}", containerFactory = "kafkaListenerContainerFactory")
    public void consumeMessage(@Header(KafkaHeaders.RECEIVED_TOPIC) String topic, String message, Acknowledgment ack) {
        try {
            receivedCount.incrementAndGet();
            receivedBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);

//            KafkaMessageEntity entity = KafkaMessageEntity.builder()
//                    .id(UUID.randomUUID())
//                    .topic(topic)
//                    .payload(message)
//                    .receivedAt(Instant.now())
//                    .build();
//            messageRepository.save(entity);

            // Acknowledge the message after successful processing
            ack.acknowledge();
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