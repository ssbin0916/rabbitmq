package com.example.rabbitmq.service;

import com.example.rabbitmq.config.RabbitMqConfig;
import com.example.rabbitmq.dto.SensorData;
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
    private final IotService iotService;

    // RabbitMQ로 메시지 발송
    public void sendMessage(String message) {
        try {
            rabbitTemplate.convertAndSend(RabbitMqConfig.IOT_EXCHANGE, RabbitMqConfig.IOT_ROUTING_KEY, message);
            log.info("메시지가 RabbitMQ로 전송되었습니다: {}", message);
        } catch (Exception e) {
            log.error("RabbitMQ로 메시지 전송 중 오류 발생: {}", e.getMessage());
            throw e;
        }
    }
    
    // RabbitMQ에서 메시지 수신
    @RabbitListener(queues = RabbitMqConfig.IOT_QUEUE)
    public void receiveMessage(String message) {
        log.info("RabbitMQ로부터 메시지 수신: {}", message);
        
        try {
            // SensorData로 변환 시도
            SensorData sensorData = objectMapper.readValue(message, SensorData.class);
            log.info("센서 데이터 처리: 장치={}, 온도={}, 습도={}", 
                    sensorData.getDeviceId(), 
                    sensorData.getTemperature(), 
                    sensorData.getHumidity());
            
            // 메시지 처리 로직
            iotService.saveMessage("iot/data", message);
            iotService.updateBrokerStatus(true);
            
        } catch (Exception e) {
            log.warn("메시지를 SensorData로 변환할 수 없습니다. 일반 메시지로 처리합니다: {}", e.getMessage());
            // 일반 메시지로 처리
            iotService.saveMessage("iot/data", message);
        }
    }
}