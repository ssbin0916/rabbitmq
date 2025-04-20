package com.example.rabbitmq.controller;

import com.example.rabbitmq.dto.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/sensor")
@RequiredArgsConstructor
public class SensorController {

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.sensor.exchange.name}")
    private String sensorExchange;

    @Value("${rabbitmq.sensor.routing.key}")
    private String sensorRoutingKey;

    @PostMapping("/data")
    public void sendSensorData(@RequestBody SensorData sensorData) {
        rabbitTemplate.convertAndSend(sensorExchange, 
                "sensor.data." + sensorData.getDeviceId(), 
                sensorData);
    }
}