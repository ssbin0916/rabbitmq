package com.example.rabbitmq.service;

import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicLong;

import static java.nio.charset.StandardCharsets.UTF_8;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttToKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic}")
    private String kafkaTopic;

    private final AtomicLong rabbitReceivedCount = new AtomicLong();
    private final AtomicLong rabbitReceivedBytes = new AtomicLong();
    // Kafka 전송 통계
    private final AtomicLong kafkaSentCount = new AtomicLong();
    private final AtomicLong kafkaSentBytes = new AtomicLong();

    @RabbitListener(
            queues = "${spring.rabbitmq.queue}",
            containerFactory = "rabbitListenerContainerFactory"
    )
    public void consumeFromMqttAndProduceToKafka(
            String message,
            Channel channel,
            @Header(AmqpHeaders.DELIVERY_TAG) long tag
    ) throws IOException {
        // 수신 통계 업데이트
        rabbitReceivedCount.incrementAndGet();
        rabbitReceivedBytes.addAndGet(message.getBytes(UTF_8).length);


//        try {
//            // Kafka로 비동기 전송 (CompletableFuture API)
//            kafkaTemplate.send(kafkaTopic, message)
//                    .whenComplete((result, ex) -> {
//                        if (ex != null) {
//                            log.error("[Kafka][전송 실패] {}", ex.toString());
//                            try {
//                                channel.basicNack(tag, false, true);
//                            } catch (IOException e) {
//                                log.error("[Rabbit][NACK 실패] {}", e.toString());
//                            }
//                        } else {
//                            try {
//                                channel.basicAck(tag, false);
//                            } catch (IOException e) {
//                                log.error("[Rabbit][ACK 실패] {}", e.toString());
//                            }
//                            long sentCount = this.sentCount.incrementAndGet();
//                            long sentBytes = this.sentBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
//                            if (sentCount % 1000 == 0) {
//                                log.info("[Kafka][전송] 누적: {}건, {} bytes", sentCount, sentBytes);
//                            }
//                        }
//                    });
//        } catch (Exception e) {
//            log.error("Kafka 전송 예외:", e);
//            channel.basicNack(tag, false, true);
//        }

//        try {
//            // 2) 동기 Kafka 전송
//            kafkaTemplate.send(kafkaTopic, message).get();
//
//            // 3) ACK
//            channel.basicAck(tag, false);
//
//            // 4) 전송 통계
//            long sc = sentCount.incrementAndGet();
//            long sb = sentBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
//            if (sc % 1000 == 0) {
//                log.info("[Kafka][전송] 누적: {}건, {} bytes", sc, sb);
//            }
//        } catch (Exception e) {
//            log.error("[Kafka 전송 실패] {}", e.toString());
//            channel.basicNack(tag, false, true);
//        }

        try {
            kafkaTemplate.send(kafkaTopic, message).get();
            channel.basicAck(tag, false);
        } catch (Exception ex) {
            log.error("[Kafka 전송 실패] {}", ex.toString());
            channel.basicNack(tag, false, true);
            return;
        }

        // 3) Kafka 전송 통계
        kafkaSentCount.incrementAndGet();
        kafkaSentBytes.addAndGet(message.getBytes(UTF_8).length);

    }

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
