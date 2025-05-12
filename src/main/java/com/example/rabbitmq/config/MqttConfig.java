package com.example.rabbitmq.config;

import org.eclipse.paho.client.mqttv3.MqttConnectOptions;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.integration.annotation.MessagingGateway;
import org.springframework.integration.annotation.ServiceActivator;
import org.springframework.integration.channel.DirectChannel;
import org.springframework.integration.core.MessageProducer;
import org.springframework.integration.mqtt.core.DefaultMqttPahoClientFactory;
import org.springframework.integration.mqtt.core.MqttPahoClientFactory;
import org.springframework.integration.mqtt.inbound.MqttPahoMessageDrivenChannelAdapter;
import org.springframework.integration.mqtt.outbound.MqttPahoMessageHandler;
import org.springframework.integration.mqtt.support.DefaultPahoMessageConverter;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.MessageHandler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor;

@Configuration
public class MqttConfig {

    @Value("${mqtt.broker-url}")
    private String brokerUrl;

    @Value("${mqtt.client-id}")
    private String clientId;

    @Value("${mqtt.topic}")
    private String topic;

    @Value("${mqtt.qos}")
    private int qos;

    @Value("${mqtt.clean-session}")
    private boolean cleanSession;

    @Bean
    public MqttPahoClientFactory mqttClientFactory() {
        DefaultMqttPahoClientFactory factory = new DefaultMqttPahoClientFactory();

        MqttConnectOptions options = new MqttConnectOptions();
        options.setServerURIs(new String[]{brokerUrl});
        options.setCleanSession(cleanSession);
        options.setKeepAliveInterval(90);
        options.setAutomaticReconnect(true);
        options.setConnectionTimeout(30);

        options.setMaxInflight(10000);

        factory.setConnectionOptions(options);
        return factory;
    }

    @Bean
    @ServiceActivator(inputChannel = "mqttExecutorChannel")
    public MessageHandler mqttPahoMessageHandler(MqttPahoClientFactory factory) {
        MqttPahoMessageHandler handler =
                new MqttPahoMessageHandler(clientId + "-out", factory);
        handler.setAsync(true);
        handler.setDefaultQos(qos);
        handler.setCompletionTimeout(5_000);

        // **여기**서 String payload → byte[] 로 변환해 줄 컨버터를 등록
        DefaultPahoMessageConverter converter = new DefaultPahoMessageConverter();
        // String 을 그대로 payload 로 쓰려면
        converter.setPayloadAsBytes(false);
        handler.setConverter(converter);

        return handler;
    }

    @Bean
    public ThreadPoolTaskExecutor mqttExecutor() {
        ThreadPoolTaskExecutor tx = new ThreadPoolTaskExecutor();
        tx.setCorePoolSize(20);
        tx.setMaxPoolSize(50);
        tx.setQueueCapacity(5_000);
        tx.setThreadNamePrefix("mqtt-send-");
        tx.initialize();
        return tx;
    }

    @MessagingGateway(defaultRequestChannel = "mqttExecutorChannel")
    public interface MqttGateway {
        void sendToMqtt(Message<String> msg);
    }
}