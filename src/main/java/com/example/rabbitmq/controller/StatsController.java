package com.example.rabbitmq.controller;

import com.example.rabbitmq.service.KafkaService;
import com.example.rabbitmq.service.MqttService;
import com.example.rabbitmq.service.MqttToKafkaService;
import com.example.rabbitmq.service.RabbitMqService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.LinkedHashMap;
import java.util.Map;

@RestController
@RequestMapping("/stats")
@RequiredArgsConstructor
public class StatsController {

    private final MqttService mqttService;
    private final RabbitMqService rabbitMqService;
    private final MqttToKafkaService pipelineService;
    private final KafkaService kafkaService;

    /** 전체 통계 조회 */
    @GetMapping
    public Map<String, Object> allStats() {
        Map<String, Object> m = new LinkedHashMap<>();
        // MQTT → RabbitMQ
        m.put("mqtt.sent.count",       mqttService.getSentCount());
        m.put("mqtt.sent.bytes",       mqttService.getSentBytes());
        m.put("rabbit.sent.count",     rabbitMqService.getSentCount());
        m.put("rabbit.sent.bytes",     rabbitMqService.getSentBytes());

        // RabbitMQ → Kafka (Pipeline)
        m.put("pipeline.received.count", pipelineService.getRabbitReceivedCount());
        m.put("pipeline.received.bytes", pipelineService.getRabbitReceivedBytes());
        m.put("pipeline.sent.count",     pipelineService.getKafkaSentCount());
        m.put("pipeline.sent.bytes",     pipelineService.getKafkaSentBytes());

        // Kafka 최종 수신
        m.put("kafka.received.count",  kafkaService.getReceivedCount());
        m.put("kafka.received.bytes",  kafkaService.getReceivedBytes());
        return m;
    }

    /** MQTT 통계 부분 조회 */
    @GetMapping("/mqtt")
    public Map<String, Long> mqttStats() {
        return Map.of(
                "sentCount", mqttService.getSentCount(),
                "sentBytes", mqttService.getSentBytes()
        );
    }

    /** RabbitMQ 통계 부분 조회 */
    @GetMapping("/rabbit")
    public Map<String, Long> rabbitStats() {
        return Map.of(
                "receivedCount", rabbitMqService.getReceivedCount(),
                "receivedBytes", rabbitMqService.getReceivedBytes(),
                "sentCount",     rabbitMqService.getSentCount(),
                "sentBytes",     rabbitMqService.getSentBytes()
        );
    }

    /** Pipeline (Rabbit→Kafka) 통계 조회 */
    @GetMapping("/pipeline")
    public Map<String, Long> pipelineStats() {
        return Map.of(
                "receivedCount", pipelineService.getRabbitReceivedCount(),
                "receivedBytes", pipelineService.getRabbitReceivedBytes(),
                "sentCount",     pipelineService.getKafkaSentCount(),
                "sentBytes",     pipelineService.getKafkaSentBytes()
        );
    }

    /** Kafka 통계 부분 조회 */
    @GetMapping("/kafka")
    public Map<String, Long> kafkaStats() {
        return Map.of(
                "receivedCount", kafkaService.getReceivedCount(),
                "receivedBytes", kafkaService.getReceivedBytes()
        );
    }

    /** 전체 통계 초기화 */
    @PostMapping("/reset")
    public void resetAll() {
        mqttService.reset();
        rabbitMqService.reset();
        pipelineService.reset();
        kafkaService.reset();
    }

    /** MQTT 통계 초기화 */
    @PostMapping("/mqtt/reset")
    public void resetMqtt() {
        mqttService.reset();
    }

    /** RabbitMQ 통계 초기화 */
    @PostMapping("/rabbit/reset")
    public void resetRabbit() {
        rabbitMqService.reset();
    }

    /** Pipeline 통계 초기화 */
    @PostMapping("/pipeline/reset")
    public void resetPipeline() {
        pipelineService.reset();
    }

    /** Kafka 통계 초기화 */
    @PostMapping("/kafka/reset")
    public void resetKafka() {
        kafkaService.reset();
    }
}