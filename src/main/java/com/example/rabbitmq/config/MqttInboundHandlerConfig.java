package com.example.rabbitmq.config;

import com.example.rabbitmq.service.RabbitMqService;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.messaging.MessageHandler;

@Configuration
@RequiredArgsConstructor
public class MqttInboundHandlerConfig {

    private final RabbitMqService rabbitMqService;

    @Bean
    @ServiceActivator(inputChannel = "mqttInputChannel")
    public MessageHandler mqttInboundHandler() {
        return message -> {
            String json = message.getPayload().toString();
            rabbitMqService.sendMessage(json);
        };
    }
}
