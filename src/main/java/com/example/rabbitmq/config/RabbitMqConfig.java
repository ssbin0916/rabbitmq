package com.example.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    // MQTT 토픽을 매핑할 RabbitMQ 큐 이름
    public static final String IOT_QUEUE = "iot.queue";
    public static final String IOT_EXCHANGE = "iot.exchange";
    public static final String IOT_ROUTING_KEY = "iot.data";

    @Bean
    public Queue iotQueue() {
        return new Queue(IOT_QUEUE, true);
    }

    @Bean
    public TopicExchange iotExchange() {
        return new TopicExchange(IOT_EXCHANGE);
    }

    @Bean
    public Binding iotBinding(Queue iotQueue, TopicExchange iotExchange) {
        return BindingBuilder.bind(iotQueue).to(iotExchange).with(IOT_ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public AmqpTemplate amqpTemplate(ConnectionFactory connectionFactory) {
        final RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}