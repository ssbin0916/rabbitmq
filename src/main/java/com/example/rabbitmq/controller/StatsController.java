package com.example.rabbitmq.controller;

import com.example.rabbitmq.service.*;
import com.example.rabbitmq.util.StatsCollector;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {


    private final MqttService mqttService;
    private final RabbitMqService rabbitService;
    private final MqttToKafkaService mqttToKafkaService;
    private final KafkaService kafkaService;
    private final StatsCollector statsCollector;
    private final TopicAdminService topicAdminService;

    @GetMapping
    public Map<String, Object> allStats() throws ExecutionException, InterruptedException {
        Map<String, Object> m = new LinkedHashMap<>();

        m.put("mqtt.sent.count", mqttService.getSentCount());
        m.put("mqtt.sent.bytes", mqttService.getSentBytes());
        m.put("mqtt.rate.count/s", statsCollector.getMqttRateCount());

        m.put("rabbit.receive.count", mqttToKafkaService.getRabbitReceivedCount());
        m.put("rabbit.receive.bytes", mqttToKafkaService.getRabbitReceivedBytes());
        m.put("rabbit.rate.count/s", statsCollector.getRabbitRateCount());

        m.put("rabbit.to.kafka.sent.count", mqttToKafkaService.getKafkaSentCount());
        m.put("rabbit.to.kafka.sent.bytes", mqttToKafkaService.getKafkaSentBytes());
        m.put("rabbit.to.kafka.rate.count/s", statsCollector.getPipelineRateCount());

        m.put("kafka.receive.count", kafkaService.getReceivedCount());
        m.put("kafka.receive.bytes", kafkaService.getReceivedBytes());
        m.put("kafka.rate.count/s", statsCollector.getKafkaRateCount());

        m.put("kafka.topic.size", topicAdminService.getTopicMessageCount());

        return m;
    }

    @GetMapping("/mqtt")
    public Map<String, Long> mqttStats() {
        return Map.of(
                "sentCount", mqttService.getSentCount(),
                "sentBytes", mqttService.getSentBytes(),
                "rateCnt/s", statsCollector.getMqttRateCount()
        );
    }

    @GetMapping("/rabbit")
    public Map<String, Long> rabbitStats() {
        return Map.of(
                "recvCount", mqttToKafkaService.getRabbitReceivedCount(),
                "recvBytes", rabbitService.getReceivedBytes(),
                "rateCnt/s", statsCollector.getRabbitRateCount()
        );
    }

    @GetMapping("/pipeline")
    public Map<String, Long> pipelineStats() {
        return Map.of(
                "sentCount", mqttToKafkaService.getKafkaSentCount(),
                "sentBytes", mqttToKafkaService.getKafkaSentBytes(),
                "rateCnt/s", statsCollector.getPipelineRateCount()
        );
    }

    @GetMapping("/kafka")
    public Map<String, Long> kafkaStats() {
        return Map.of(
                "recvCount", kafkaService.getReceivedCount(),
                "recvBytes", kafkaService.getReceivedBytes(),
                "rateCnt/s", statsCollector.getKafkaRateCount()
        );
    }

}