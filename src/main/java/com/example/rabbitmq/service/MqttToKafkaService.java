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
import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttToKafkaService {

    private final KafkaTemplate<String, String> kafkaTemplate;

    @Value("${spring.kafka.topic}")
    private String kafkaTopic;

    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicLong sentCount = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();

//    @RabbitListener(queues = "${spring.rabbitmq.queue}", containerFactory = "rabbitListenerContainerFactory")
//    public void consumeFromMqttAndProduceToKafka(String message) {
//        try {
//            kafkaTemplate.send("kafka-test-topic", message);
//        } catch (Exception e) {
//            log.error("Kafka 전송 실패: {}", e.getMessage());
//        }
//    }

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
        long receiveCount = receivedCount.incrementAndGet();
        long receiveBytes = receivedBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
        if (receiveCount % 1000 == 0) {
            log.info("[Rabbit][수신] 누적: {}건, {} bytes", receiveCount, receiveBytes);
        }

        try {
            // Kafka로 비동기 전송 (CompletableFuture API)
            kafkaTemplate.send(kafkaTopic, message)
                    .whenComplete((result, ex) -> {
                        if (ex != null) {
                            log.error("[Kafka][전송 실패] {}", ex.toString());
                            try {
                                channel.basicNack(tag, false, true);
                            } catch (IOException e) {
                                log.error("[Rabbit][NACK 실패] {}", e.toString());
                            }
                        } else {
                            try {
                                channel.basicAck(tag, false);
                            } catch (IOException e) {
                                log.error("[Rabbit][ACK 실패] {}", e.toString());
                            }
                            long sc = sentCount.incrementAndGet();
                            long sb = sentBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
                            if (sc % 1000 == 0) {
                                log.info("[Kafka][전송] 누적: {}건, {} bytes", sc, sb);
                            }
                        }
                    });
        } catch (Exception e) {
            log.error("Kafka 전송 예외:", e);
            channel.basicNack(tag, false, true);
        }
    }

    /**
     * 5초마다 수신/전송 통계 로그를 출력합니다.
     */
    @Scheduled(fixedRate = 5000)
    public void report() {
        log.info("[Pipeline][5s] Rabbit→Kafka 수신={}건({}B), 전송={}건({}B)",
                receivedCount.get(), receivedBytes.get(),
                sentCount.get(), sentBytes.get());
    }

    public long getReceivedCount() {
        return receivedCount.get();
    }

    public long getReceivedBytes() {
        return receivedBytes.get();
    }

    public long getSentCount() {
        return sentCount.get();
    }

    public long getSentBytes() {
        return sentBytes.get();
    }

    public void reset() {
        receivedCount.set(0);
        receivedBytes.set(0);
        sentCount.set(0);
        sentBytes.set(0);
    }

}
