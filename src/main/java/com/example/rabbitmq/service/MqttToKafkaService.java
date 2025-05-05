package com.example.rabbitmq.service;

import com.example.rabbitmq.metrics.Metrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttToKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;
    private final Metrics metrics = new Metrics();

    @RabbitListener(queues = "${spring.rabbitmq.queue}", concurrency = "8") // 8개의 스레드로 소비
    public void consumeFromMqttAndProduceToKafka(String message) {
        try {
            long start =System.nanoTime();
            kafkaTemplate.send("kafka-test-topic", message);
            metrics.getKafkaSentCount().incrementAndGet();
            metrics.getKafkaSendTime().addAndGet(System.nanoTime() - start);
        } catch (Exception e) {
            log.error("Kafka 전송 실패: {}", e.getMessage());
        }
    }

}
