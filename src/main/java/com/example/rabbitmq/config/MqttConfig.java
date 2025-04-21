package com.example.rabbitmq.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
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

    // application.yml 또는 properties에서 설정 값을 주입받음
    @Value("${mqtt.broker.url}")
    private String brokerUrl; // MQTT 브로커 주소 (ex. tcp://localhost:1883)

    @Value("${mqtt.client.id}")
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
        options.setServerURIs(new String[] { brokerUrl }); // 연결할 브로커 주소
        options.setCleanSession(cleanSession); // 클린 세션 설정
        options.setKeepAliveInterval(90); // 연결 유지 간격 (초)
        options.setAutomaticReconnect(true); // 자동 재연결 설정
        options.setConnectionTimeout(30); // 연결 타임아웃 (초)

        factory.setConnectionOptions(options);
        return factory;
    }

    /**
     * MQTT 메시지를 수신하는 인바운드 채널입니다.
     * 메시지를 어댑터에서 받아서 이 채널을 통해 처리됩니다.
     */
    @Bean
    public MessageChannel mqttInputChannel() {
        return new DirectChannel(); // 단순하게 메시지를 한 소비자로 전달
    }

    /**
     * MQTT 메시지를 발행하는 아웃바운드 채널입니다.
     * 클라이언트가 메시지를 보내고자 할 때 이 채널을 통해 보냅니다.
     */
    @Bean
    public MessageChannel mqttOutboundChannel() {
        return new DirectChannel();
    }

    /**
     * MQTT로부터 메시지를 수신하는 어댑터입니다.
     * 설정된 토픽을 구독하며, 수신된 메시지를 mqttInputChannel로 보냅니다.
     */
    @Bean
    public MessageProducer inbound() {
        // 수신용 클라이언트 ID는 "-in"을 붙여 중복 방지
        MqttPahoMessageDrivenChannelAdapter adapter =
                new MqttPahoMessageDrivenChannelAdapter(clientId + "-in", mqttClientFactory(), topic);

        adapter.setCompletionTimeout(5000); // 연결 응답 타임아웃
        adapter.setConverter(new DefaultPahoMessageConverter()); // 기본 메시지 컨버터 사용
        adapter.setQos(qos); // QoS 설정
        adapter.setOutputChannel(mqttInputChannel()); // 수신 메시지 전달 채널

        return adapter;
    }

    /**
     * MQTT로 메시지를 발행하는 핸들러입니다.
     * mqttOutboundChannel로 전달된 메시지를 MQTT 브로커로 전송합니다.
     */
    @Bean
    @ServiceActivator(inputChannel = "mqttOutboundChannel")
    public MessageHandler mqttOutbound() {
        // 발신용 클라이언트 ID는 "-out"을 붙여 중복 방지
        MqttPahoMessageHandler messageHandler =
                new MqttPahoMessageHandler(clientId + "-out", mqttClientFactory());

        messageHandler.setAsync(true); // 비동기 방식으로 전송
        messageHandler.setDefaultTopic(topic); // 기본 토픽 설정
        messageHandler.setDefaultQos(qos); // 기본 QoS 설정

        return messageHandler;
    }
}