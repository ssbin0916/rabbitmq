package com.example.rabbitmq.service;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttToKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic}")
    private String kafkaTopic;

    private final AtomicLong rabbitReceivedCount = new AtomicLong();
    private final AtomicLong rabbitReceivedBytes = new AtomicLong();

    private final AtomicLong kafkaSentCount = new AtomicLong();
    private final AtomicLong kafkaSentBytes = new AtomicLong();

    /**
     * 배치 모드로 RabbitMQ → Kafka
     */
    @RabbitListener(
            queues = "${spring.rabbitmq.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void handleBatch(List<Message> messages, Channel channel) throws IOException {
        int number = messages.size();
        long batchBytes = 0L;

        // 1) 수신 통계
        for (Message message : messages) {
            byte[] body = message.getBody();
            batchBytes += body.length;
        }
        rabbitReceivedCount.addAndGet(number);
        rabbitReceivedBytes.addAndGet(batchBytes);

        // 2) Kafka로 일괄 전송 & 전송 통계
        long sentBatchBytes = 0L;
        for (Message message : messages) {
            String payload = new String(message.getBody(), StandardCharsets.UTF_8);
            kafkaTemplate.send(kafkaTopic, payload);
            sentBatchBytes += payload.getBytes(StandardCharsets.UTF_8).length;
        }
        kafkaSentCount.addAndGet(number);
        kafkaSentBytes.addAndGet(sentBatchBytes);

        // 3) 마지막 deliveryTag까지 한 번에 ACK
        long lastTag = messages.get(number - 1)
                .getMessageProperties()
                .getDeliveryTag();
        channel.basicAck(lastTag, true);
    }

    // getters for count/bytes
    public long getRabbitReceivedCount() {
        return rabbitReceivedCount.get();
    }

    public long getRabbitReceivedBytes() {
        return rabbitReceivedBytes.get();
    }

    public long getKafkaSentCount() {
        return kafkaSentCount.get();
    }

    public long getKafkaSentBytes() {
        return kafkaSentBytes.get();
    }

    public void reset() {
        rabbitReceivedCount.set(0);
        rabbitReceivedBytes.set(0);
        kafkaSentCount.set(0);
        kafkaSentBytes.set(0);
    }
}