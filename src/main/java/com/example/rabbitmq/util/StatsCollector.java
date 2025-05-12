package com.example.rabbitmq.util;

import com.example.rabbitmq.service.KafkaService;
import com.example.rabbitmq.service.MqttService;
import com.example.rabbitmq.service.MqttToKafkaService;
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
    private final MqttToKafkaService mqttToKafkaService;
    private final KafkaService kafkaService;

    private long prevMqttCnt;
    private long prevRabbitCnt;
    private long prevPipelineCnt;
    private long prevKafkaCnt;
    private Instant prevTime = Instant.now();

    private final AtomicLong mqttRateCnt     = new AtomicLong();
    private final AtomicLong rabbitRateCnt   = new AtomicLong();
    private final AtomicLong pipelineRateCnt = new AtomicLong();
    private final AtomicLong kafkaRateCnt    = new AtomicLong();

    @PostConstruct
    public void init() {
        prevMqttCnt     = mqttService.getSentCount();
        prevRabbitCnt   = mqttToKafkaService.getRabbitReceivedCount();
        prevPipelineCnt = mqttToKafkaService.getKafkaSentCount();
        prevKafkaCnt    = kafkaService.getReceivedCount();
    }

    @Scheduled(fixedRate = 1000)
    public void sampleRates() {
        Instant now = Instant.now();
        double sec = (now.toEpochMilli() - prevTime.toEpochMilli()) / 1_000.0;

        long mCnt = mqttService.getSentCount();
        mqttRateCnt.set((long) ((mCnt - prevMqttCnt) / sec));
        prevMqttCnt = mCnt;

        long rCnt = mqttToKafkaService.getRabbitReceivedCount();
        rabbitRateCnt.set((long) ((rCnt - prevRabbitCnt) / sec));
        prevRabbitCnt = rCnt;

        long pCnt = mqttToKafkaService.getKafkaSentCount();
        pipelineRateCnt.set((long) ((pCnt - prevPipelineCnt) / sec));
        prevPipelineCnt = pCnt;

        long kCnt = kafkaService.getReceivedCount();
        kafkaRateCnt.set((long) ((kCnt - prevKafkaCnt) / sec));
        prevKafkaCnt = kCnt;

        prevTime = now;
    }

    // getters for count rates only
    public long getMqttRateCount()    { return mqttRateCnt.get(); }
    public long getRabbitRateCount()  { return rabbitRateCnt.get(); }
    public long getPipelineRateCount(){ return pipelineRateCnt.get(); }
    public long getKafkaRateCount()   { return kafkaRateCnt.get(); }
}
