package com.example.rabbitmq.service;

import com.example.rabbitmq.dto.SensorData;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import java.time.LocalDateTime;
import java.util.Random;

@Service
@EnableScheduling
@RequiredArgsConstructor
public class SensorDataSimulator {
    private final Random random = new Random();
    private final RestTemplate restTemplate = new RestTemplate();
    
    @Scheduled(fixedDelay = 1000) // 1초마다 실행
    public void generateData() {
        SensorData data = new SensorData();
        data.setDeviceId("device001");
        data.setTemperature(20.0 + random.nextDouble() * 10.0); // 20-30도 사이
        data.setHumidity(40.0 + random.nextDouble() * 40.0); // 40-80% 사이
        data.setTimestamp(LocalDateTime.now());
        
        restTemplate.postForObject(
            "http://localhost:8080/api/sensor/data",
            data,
            Void.class
        );
    }
}