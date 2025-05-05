package com.example.rabbitmq.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl; // MQTT 브로커 주소 (ex. tcp://localhost:1883)

    @Value("${mqtt.client-id}")
    private String clientId; // 클라이언트 ID (수신/발신용으로 나누어 사용)

    @Value("${mqtt.topic}")
    private String topic; // 구독 및 발행에 사용할 기본 토픽

    @Value("${mqtt.qos}")
    private int qos; // QoS (0: at most once, 1: at least once, 2: exactly once)

    @Value("${mqtt.clean-session}")
    private boolean cleanSession; // 클린 세션 설정 (true면 연결 종료 시 구독 정보 제거)

    /**
     * MQTT 연결에 사용할 클라이언트 팩토리 설정입니다.
     * 연결 옵션(브로커 주소, 세션, 재연결 등)을 지정합니다.
     */
    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl}); // 연결할 브로커 주소
        options.setCleanSession(cleanSession); // 클린 세션 설정
        options.setKeepAliveInterval(90); // 연결 유지 간격 (초)
        options.setAutomaticReconnect(true); // 자동 재연결 설정
        options.setConnectionTimeout(30); // 연결 타임아웃 (초)

        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel(); // 단순하게 메시지를 한 소비자로 전달
    }

    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    @Bean
    public MessageProducer inbound() {
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-in", mqttClientFactory(), topic);

        adapter.setCompletionTimeout(5000);
        adapter.setConverter(new DefaultPahoMessageConverter());
        adapter.setQos(qos);
        adapter.setOutputChannel(mqttInputChannel());
        return adapter;
    }

    @Bean
    public MessageHandler mqttOutbound() {
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId + "-out", mqttClientFactory());

        messageHandler.setAsync(true);
        messageHandler.setDefaultTopic(topic);
        messageHandler.setDefaultQos(qos);

        return messageHandler;
    }
}