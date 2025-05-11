package com.example.rabbitmq.service;

import com.example.rabbitmq.config.RabbitMqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rabbitmq.client.Channel;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.AmqpHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;
    private final KafkaService kafkaService;

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

    // RabbitMQ에서 메시지 수신
//    @RabbitListener(queues = "${spring.rabbitmq.queue}")
//    public void receiveMessage(String message, Channel channel, @Header(AmqpHeaders.DELIVERY_TAG) long tag) throws IOException {
//        long receiveCount = receivedCount.incrementAndGet();
//        long receiveBytes = receivedBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
//        if(receiveCount % 1000 == 0) {
//            log.info("[Rabbit][수신] 누적: {}건, {} bytes", receiveCount, receiveBytes);
//        }
//
//        kafkaService.sendMessage("kafka-test-topic", message);
//        channel.basicAck(tag, false);
//
//        long sentCount = this.sentCount.incrementAndGet();
//        long sentBytes = this.sentBytes.addAndGet(message.getBytes(StandardCharsets.UTF_8).length);
//        if (sentCount % 1000 == 0) {
//            log.info("[Rabbit][전송] 누적: {}건, {} bytes", sentCount, sentBytes);
//        }
//    }

    @Scheduled(fixedRate = 5000)
    public void report() {
        log.info("[Rabbit][5s] 수신={}건({}B), 전송={}건({}B)",
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