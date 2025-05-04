package com.example.rabbitmq.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMqConfig {

    public static String QUEUE;
    public static String EXCHANGE;
    public static String ROUTING_KEY;

    @Value("${spring.rabbitmq.queue}")
    public void setQueue(String queue) {
        QUEUE = queue;
    }

    @Value("${spring.rabbitmq.exchange}")
    public void setExchange(String exchange) {
        EXCHANGE = exchange;
    }

    @Value("${spring.rabbitmq.routing-key}")
    public void setRoutingKey(String routingKey) {
        ROUTING_KEY = routingKey;
    }

    @Bean
    public Queue queue() {
        return new Queue(QUEUE, true);
    }

    @Bean
    public TopicExchange exchange() {
        return new TopicExchange(EXCHANGE);
    }

    @Bean
    public Binding binding(Queue iotQueue, TopicExchange iotExchange) {
        return BindingBuilder.bind(iotQueue).to(iotExchange).with(ROUTING_KEY);
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}