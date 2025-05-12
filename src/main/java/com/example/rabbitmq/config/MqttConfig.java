package com.example.rabbitmq.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

/**
 * MQTT 설정 클래스.
 * Spring Integration MQTT를 이용해 비동기/동기 메시지 발행을 구성합니다.
 */
@Configuration
public class MqttConfig {

    /**
     * MQTT 브로커 URL (e.g., tcp://localhost:1883)
     */
    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    /**
     * 클라이언트 ID (브로커에 연결 시 식별자)
     */
    @Value("${mqtt.client-id}")
    private String clientId;

    /**
     * 기본 토픽 (별도 설정이 없을 때 사용)
     */
    @Value("${mqtt.topic}")
    private String topic;

    /**
     * Quality of Service 레벨 (0, 1, 2)
     */
    @Value("${mqtt.qos}")
    private int qos;

    /**
     * 세션 유지 여부 (true: 세션 무효화, false: 세션 지속)
     */
    @Value("${mqtt.clean-session}")
    private boolean cleanSession;

    /**
     * MQTT 클라이언트 팩토리 생성
     * - 연결 옵션(서버 URI, 클린세션, 자동 재연결 등)을 설정
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();
        MqttConnectOptions options = new MqttConnectOptions();

        // 브로커 주소 설정
        options.setServerURIs(new String[]{brokerUrl});
        // 세션 관리 (true = 매번 새로운 세션)
        options.setCleanSession(cleanSession);
        // Keep-Alive 간격 (초)
        options.setKeepAliveInterval(90);
        // 네트워크 오류 시 자동 재연결
        options.setAutomaticReconnect(true);
        // 연결 타임아웃 (초)
        options.setConnectionTimeout(30);
        // 최대 Inflight 메시지 수 (동시 발행)
        options.setMaxInflight(10000);

        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * MQTT 발행 MessageHandler
     * - inputChannel: mqttExecutorChannel
     * - 비동기 발행 모드
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttExecutorChannel")
    public MessageHandler mqttPahoMessageHandler(MqttPahoClientFactory factory) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(clientId + "-out", factory);

        // 비동기 전송 여부
        handler.setAsync(true);
        // 기본 QoS 설정
        handler.setDefaultQos(qos);
        // 전송 완료 대기 타임아웃 (ms)
        handler.setCompletionTimeout(5_000);

        // 페이로드 변환 설정 (문자열 기반)
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        converter.setPayloadAsBytes(false);
        handler.setConverter(converter);

        return handler;
    }

    /**
     * ThreadPoolTaskExecutor
     * - MQTT 발행을 위한 스레드 풀
     */
    @Bean
    public ThreadPoolTaskExecutor mqttExecutor() {
        ThreadPoolTaskExecutor tx = new ThreadPoolTaskExecutor();
        // 최소 스레드 수
        tx.setCorePoolSize(20);
        // 최대 스레드 수
        tx.setMaxPoolSize(50);
        // 대기 큐 크기
        tx.setQueueCapacity(5_000);
        // 스레드 이름 접두사
        tx.setThreadNamePrefix("mqtt-send-");
        tx.initialize();
        return tx;
    }

    /**
     * Messaging Gateway 인터페이스
     * - sendToMqtt() 호출 시 mqttExecutorChannel 로 메시지 전송
     */
    @MessagingGateway(defaultRequestChannel = "mqttExecutorChannel")
    public interface MqttGateway {
        void sendToMqtt(Message<String> msg);
    }
}
