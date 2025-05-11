package com.example.rabbitmq.service;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Objects;
import java.util.concurrent.TimeUnit;

@Service
@RequiredArgsConstructor
public class MetricsService {

    private final MeterRegistry meterRegistry;

    public void recordKafkaProcessing(long count, long millis) {
        meterRegistry.counter("kafka.processing.count").increment(count);
        meterRegistry.timer("kafka.processing.time").record(millis, java.util.concurrent.TimeUnit.MILLISECONDS);
    }

    public double getKafkaProcessedCount() {
        var counter = meterRegistry.find("kafka.processing.count").counter();
        return counter != null ? counter.count() : 0.0;
    }

    public double getKafkaProcessingTime() {
        Timer timer = meterRegistry.find("kafka.processing.time").timer();
        return timer != null ? timer.totalTime(TimeUnit.MILLISECONDS) : 0.0;
    }
}
