package com.example.rabbitmq.controller;

import com.example.rabbitmq.metrics.Metrics;
import com.example.rabbitmq.service.LoadTestService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/api/metrics")
@RequiredArgsConstructor
public class MeticsController {

    private final LoadTestService loadTestService;

    @GetMapping
    public Metrics getMetrics() {
        return loadTestService.getMetrics();}
}
