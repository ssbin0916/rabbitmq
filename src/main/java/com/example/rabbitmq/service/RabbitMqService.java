package com.example.rabbitmq.service;

import com.example.rabbitmq.config.RabbitMqConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqService {

    private final RabbitTemplate rabbitTemplate;

    private final AtomicLong receivedCount = new AtomicLong();
    private final AtomicLong receivedBytes = new AtomicLong();
    private final AtomicLong sentCount = new AtomicLong();
    private final AtomicLong sentBytes = new AtomicLong();


    // RabbitMQ로 메시지 발송
    public void sendMessage(String message) {
        rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, message);
        sentCount.incrementAndGet();
        sentBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
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