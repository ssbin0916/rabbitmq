package com.example.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * RabbitMQ 설정을 담당하는 구성 클래스입니다.
 */
@Configuration
public class RabbitMqConfig {

    // RabbitMQ 설정값을 application.yml/properties에서 주입받아 저장할 정적 변수
    public static String QUEUE;
    public static String EXCHANGE;
    public static String ROUTING_KEY;

    /**
     * application.yml의 spring.rabbitmq.queue 값을 QUEUE에 저장합니다.
     */
    @Value("${spring.rabbitmq.queue}")
    public void setQueue(String queue) {
        QUEUE = queue;
    }

    /**
     * application.yml의 spring.rabbitmq.exchange 값을 EXCHANGE에 저장합니다.
     */
    @Value("${spring.rabbitmq.exchange}")
    public void setExchange(String exchange) {
        EXCHANGE = exchange;
    }

    /**
     * application.yml의 spring.rabbitmq.routing-key 값을 ROUTING_KEY에 저장합니다.
     */
    @Value("${spring.rabbitmq.routing-key}")
    public void setRoutingKey(String routingKey) {
        ROUTING_KEY = routingKey;
    }

    /**
     * durable한 큐를 생성합니다.
     */
    @Bean
    public Queue queue() {
        return new Queue(QUEUE, true);
    }

    /**
     * 토픽 기반 교환(exchange)을 생성합니다.
     */
    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    /**
     * 큐와 교환을 라우팅 키로 바인딩(binding)합니다.
     */
    @Bean
    public Binding binding(Queue queue, TopicExchange exchange) {
        return BindingBuilder
                .bind(queue)
                .to(exchange)
                .with(ROUTING_KEY);
    }

    /**
     * RabbitListener 컨테이너 팩토리를 설정합니다.
     * - concurrentConsumers: 동시에 처리할 소비자 스레드 수 (고정)
     * - prefetchCount: 각 소비자당 한 번에 가져올 메시지 개수
     * - AcknowledgeMode.MANUAL: 수동 ACK 모드
     * - 배치 리스너 및 배치 소비 활성화
     * - 배치 사이즈 및 수신 타임아웃 설정
     */
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory
    ) {
        SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);

        // 소비자 스레드 수를 8개로 고정
        factory.setConcurrentConsumers(8);
        factory.setMaxConcurrentConsumers(8);

        // 한 번에 prefetch 하는 메시지 개수
        factory.setPrefetchCount(500);

        // 메시지 처리 완료 후 수동으로 ACK
        factory.setAcknowledgeMode(AcknowledgeMode.MANUAL);

        // 배치 리스너 모드 활성화 (List<Message> 로 한 번에 받음)
        factory.setBatchListener(true);
        factory.setConsumerBatchEnabled(true);

        // 배치당 최대 메시지 개수 및 대기 시간
        factory.setBatchSize(200);
        factory.setBatchReceiveTimeout(500L);

        return factory;
    }
}