package com.learn.appjava.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    // User constants — giữ lại vì UserEventRabbitProducer đang dùng
    public static final String USER_QUEUE = "user.queue";
    public static final String USER_NOTIFICATION_QUEUE = "user.notification.queue";
    public static final String USER_DIRECT_EXCHANGE = "user.direct.exchange";
    public static final String USER_FANOUT_EXCHANGE = "user.fanout.exchange";
    public static final String USER_TOPIC_EXCHANGE = "user.topic.exchange";
    public static final String USER_CREATED_KEY = "user.created";
    public static final String USER_UPDATED_KEY = "user.updated";

    // Order constants — chỉ giữ constants, không cần @Bean nữa
    public static final String ORDER_DIRECT_EXCHANGE = "order.direct.exchange";
    public static final String ORDER_FANOUT_EXCHANGE = "order.fanout.exchange";
    public static final String ORDER_TOPIC_EXCHANGE = "order.topic.exchange";
    public static final String ORDER_PAYMENT_KEY = "order.payment";

    // ==================== USER BEANS ====================
    @Bean
    public Queue userQueue() {
        return QueueBuilder.durable(USER_QUEUE).build();
    }

    @Bean
    public Queue userNotificationQueue() {
        return QueueBuilder.durable(USER_NOTIFICATION_QUEUE).build();
    }

    @Bean
    public DirectExchange userDirectExchange() {
        return new DirectExchange(USER_DIRECT_EXCHANGE);
    }

    @Bean
    public FanoutExchange userFanoutExchange() {
        return new FanoutExchange(USER_FANOUT_EXCHANGE);
    }

    @Bean
    public TopicExchange userTopicExchange() {
        return new TopicExchange(USER_TOPIC_EXCHANGE);
    }

    @Bean
    public Binding directBinding() {
        return BindingBuilder.bind(userQueue()).to(userDirectExchange()).with(USER_CREATED_KEY);
    }

    @Bean
    public Binding fanoutBindingUser() {
        return BindingBuilder.bind(userQueue()).to(userFanoutExchange());
    }

    @Bean
    public Binding fanoutBindingNotification() {
        return BindingBuilder.bind(userNotificationQueue()).to(userFanoutExchange());
    }

    @Bean
    public Binding topicBinding() {
        return BindingBuilder.bind(userQueue()).to(userTopicExchange()).with("user.#");
    }

    // ==================== CONVERTER ====================
    @Bean
    public Jackson2JsonMessageConverter messageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory factory) {
        RabbitTemplate template = new RabbitTemplate(factory);
        template.setMessageConverter(messageConverter());
        return template;
    }
}