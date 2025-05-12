package com.example.rabbitmq.service;

import com.example.rabbitmq.config.MqttConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.messaging.Message;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Slf4j
@Service
@RequiredArgsConstructor
public class MqttService {

    private final MqttConfig.MqttGateway mqttGateway;
    private final AtomicLong sentCount = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();

    public void sendMessage(Message<String> message) {
        mqttGateway.sendToMqtt(message);
        sentCount.incrementAndGet();
        sentBytes.addAndGet(message.getPayload().getBytes(StandardCharsets.UTF_8).length);
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
