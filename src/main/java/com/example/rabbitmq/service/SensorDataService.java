package com.example.rabbitmq.service;

import com.example.rabbitmq.dto.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SensorDataService {
    private final SimpMessagingTemplate messagingTemplate;

    @RabbitListener(queues = "${rabbitmq.sensor.queue.name}")
    public void processSensorData(SensorData sensorData) {
        // 웹소켓을 통해 클라이언트에게 데이터 전송
        messagingTemplate.convertAndSend("/topic/sensor.data", sensorData);
    }
}