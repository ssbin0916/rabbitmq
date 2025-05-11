package com.example.rabbitmq.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.integration.mqtt.support.MqttHeaders;
import org.springframework.integration.support.MessageBuilder;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.scheduling.annotation.Async;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService {

    private final MessageChannel mqttOutboundChannel;
    private final ThreadPoolTaskExecutor mqttExecutor;
    private final AtomicLong sentCount = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();

//    public void sendMessage(String topic, String payload) {
//        mqttExecutor.execute(() -> {
//            try {
//                var msg = MessageBuilder
//                        .withPayload(payload)
//                        .setHeader(MqttHeaders.TOPIC, topic)
//                        .build();
//                mqttOutboundChannel.send(msg);
//
//                long count = sentCount.incrementAndGet();
//                long bytes = sentBytes.addAndGet(payload.getBytes(StandardCharsets.UTF_8).length);
//                if (count % 1000 == 0) {
//                    log.info("[MQTT] 누적 전송: {}건, {} bytes", count, bytes);
//                }
//            } catch (Exception e) {
//                log.error("[MQTT] 비동기 전송 에러: topic={}, error={}", topic, e.toString());
//            }
//        });
//    }

    public void sendMessage(String topic, String payload) {
        mqttExecutor.execute(() -> {
            try {
                Message<String> msg = MessageBuilder.withPayload(payload)
                        .setHeader(MqttHeaders.TOPIC, topic)
                        .build();
                mqttOutboundChannel.send(msg);
                long bytes = payload.getBytes(StandardCharsets.UTF_8).length;
                long cnt   = sentCount.incrementAndGet();
                sentBytes.addAndGet(bytes);

                if (cnt % 1000 == 0) {
                    log.info("[MQTT] 누적 전송: {}건, {} bytes", cnt, sentBytes.get());
                }
            } catch(Exception ex) {
                log.error("[MQTT] 전송 실패: {}", ex.toString());
            }
        });
    }


    public long getSentCount() {
        return sentCount.get();
    }

    public long getSentBytes() {
        return sentBytes.get();
    }

    public void reset() {
        sentCount.set(0);
        sentBytes.set(0);
    }
}
