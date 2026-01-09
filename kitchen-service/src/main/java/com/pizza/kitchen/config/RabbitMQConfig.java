package com.pizza.kitchen.config;

import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String ORDER_PLACED_QUEUE = "order.placed";
    public static final String ORDER_READY_QUEUE = "order.ready";

    @Bean
    public Queue orderPlacedQueue() {
        return new Queue(ORDER_PLACED_QUEUE, true); // durable = true
    }

    @Bean
    public Queue orderReadyQueue() {
        return new Queue(ORDER_READY_QUEUE, true); // durable = true
    }

    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory, 
                                        MessageConverter messageConverter) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(messageConverter);
        return template;
    }
}
