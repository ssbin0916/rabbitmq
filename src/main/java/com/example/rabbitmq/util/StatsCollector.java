package com.example.rabbitmq.util;

import com.example.rabbitmq.service.KafkaService;
import com.example.rabbitmq.service.MqttService;
import com.example.rabbitmq.service.MqttToKafkaService;
import com.example.rabbitmq.service.RabbitMqService;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.util.concurrent.atomic.AtomicLong;

@Component
@RequiredArgsConstructor
public class StatsCollector {

    private final MqttService mqttService;
    private final RabbitMqService rabbitMqService;
    private final MqttToKafkaService pipelineService;
    private final KafkaService kafkaService;

    // 직전 스냅샷
    private long prevMqttCount = 0, prevMqttBytes = 0;
    private long prevRabbitCount = 0, prevRabbitBytes = 0;
    private long prevPipelineCount = 0, prevPipelineBytes = 0;
    private long prevKafkaCount = 0, prevKafkaBytes = 0;
    private Instant prevTime = Instant.now();

    // 1초마다 갱신
    private AtomicLong mqttRateCnt = new AtomicLong(), mqttRateBytes = new AtomicLong();
    private AtomicLong rabbitRateCnt = new AtomicLong(), rabbitRateBytes = new AtomicLong();
    private AtomicLong pipelineRateCnt = new AtomicLong(), pipelineRateBytes = new AtomicLong();
    private AtomicLong kafkaRateCnt = new AtomicLong(), kafkaRateBytes = new AtomicLong();

    @PostConstruct
    public void init() {
        prevMqttCount = mqttService.getSentCount();
        prevMqttBytes = mqttService.getSentBytes();
        prevRabbitCount = rabbitMqService.getSentCount();
        prevRabbitBytes = rabbitMqService.getSentBytes();
        prevKafkaCount = kafkaService.getReceivedCount();
        prevKafkaBytes = kafkaService.getReceivedBytes();
    }

    @Scheduled(fixedRate = 1000)
    public void sampleRates() {
        Instant now = Instant.now();
        double elapsedSec = (now.toEpochMilli() - prevTime.toEpochMilli()) / 1000.0;

        long mCnt = mqttService.getSentCount();
        long mBytes = mqttService.getSentBytes();
        mqttRateCnt.set((long)((mCnt - prevMqttCount) / elapsedSec));
        mqttRateBytes.set((long)((mBytes - prevMqttBytes) / elapsedSec));
        prevMqttCount = mCnt; prevMqttBytes = mBytes;

        long rCnt = rabbitMqService.getSentCount();
        long rBytes = rabbitMqService.getSentBytes();
        rabbitRateCnt.set((long)((rCnt - prevRabbitCount) / elapsedSec));
        rabbitRateBytes.set((long)((rBytes - prevRabbitBytes) / elapsedSec));
        prevRabbitCount = rCnt; prevRabbitBytes = rBytes;

        long kCnt = kafkaService.getReceivedCount();
        long kBytes = kafkaService.getReceivedBytes();
        kafkaRateCnt.set((long)((kCnt - prevKafkaCount) / elapsedSec));
        kafkaRateBytes.set((long)((kBytes - prevKafkaBytes) / elapsedSec));
        prevKafkaCount = kCnt; prevKafkaBytes = kBytes;

        prevTime = now;
    }

    // Rate getters
    public long getMqttRateCount()    { return mqttRateCnt.get(); }
    public long getMqttRateBytes()    { return mqttRateBytes.get(); }
    public long getRabbitRateCount()  { return rabbitRateCnt.get(); }
    public long getRabbitRateBytes()  { return rabbitRateBytes.get(); }
    public long getPipelineRateCount(){ return pipelineRateCnt.get(); }
    public long getPipelineRateBytes(){ return pipelineRateBytes.get(); }
    public long getKafkaRateCount()   { return kafkaRateCnt.get(); }
    public long getKafkaRateBytes()   { return kafkaRateBytes.get(); }
}
