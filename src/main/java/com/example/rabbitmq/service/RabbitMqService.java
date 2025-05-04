package com.example.rabbitmq.service;

import com.example.rabbitmq.config.RabbitMqConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class RabbitMqService {

    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    // RabbitMQ로 메시지 발송
    public void sendMessage(String message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.EXCHANGE, RabbitMqConfig.ROUTING_KEY, message);
        } catch (Exception e) {
            throw e;
        }
    }

    // RabbitMQ에서 메시지 수신
    @RabbitListener(queues = "${spring.rabbitmq.queue}")
    public void receiveMessage(String message) throws JsonProcessingException {

    }
}