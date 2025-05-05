package com.example.rabbitmq.metrics;

import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

@Getter
@Setter
@ToString
public class Metrics {

    // 송신 & 수신된 메시지 수
    private AtomicInteger rabbitMqSentCount = new AtomicInteger(0);
    private AtomicInteger mqttConsumedCount = new AtomicInteger(0);
    private AtomicInteger kafkaSentCount = new AtomicInteger(0);

    // 처리 시간
    private AtomicLong rabbitMqSendTime = new AtomicLong(0);
    private AtomicLong kafkaSendTime = new AtomicLong(0);

    // JVM 메모리
    private AtomicLong jvmHeapMemoryUsed = new AtomicLong(0);
    private AtomicLong jvmHeapMemoryMax = new AtomicLong(0);

    // JVM CPU 사용량
    private AtomicLong jvmCpuLoad = new AtomicLong(0);

    public void logMetrics() {
        System.out.println(this.toString());
    }

}
