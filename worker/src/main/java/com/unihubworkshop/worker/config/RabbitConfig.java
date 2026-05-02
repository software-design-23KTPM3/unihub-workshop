package com.unihubworkshop.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitConfig {

    public static final String REGISTRATION_EXCHANGE = "registration.exchange";
    public static final String REGISTRATION_QUEUE = "registration.queue";
    public static final String REGISTRATION_ROUTING_KEY = "registration.created";

    public static final String WORKSHOP_CANCEL_QUEUE = "workshop.cancel.queue";
    public static final String WORKSHOP_CANCEL_ROUTING_KEY = "workshop.cancelled";

    public static final String NOTIFICATION_QUEUE = "notification.queue";
    public static final String NOTIFICATION_ROUTING_KEY = "notification.send";
    public static final String NOTIFICATION_EXCHANGE = "notification.exchange";

    public static final String AI_SUMMARY_QUEUE = "ai-summary.queue";
    public static final String AI_SUMMARY_ROUTING_KEY = "ai-summary.generate";
    public static final String AI_SUMMARY_EXCHANGE = "ai-summary.exchange";

    @Bean
    public DirectExchange aiSummaryExchange() {
        return new DirectExchange(AI_SUMMARY_EXCHANGE);
    }

    @Bean
    public Queue aiSummaryQueue() {
        return new Queue(AI_SUMMARY_QUEUE, true);
    }

    @Bean
    public Binding aiSummaryBinding(Queue aiSummaryQueue, DirectExchange aiSummaryExchange) {
        return BindingBuilder.bind(aiSummaryQueue).to(aiSummaryExchange).with(AI_SUMMARY_ROUTING_KEY);
    }

    @Bean
    public DirectExchange notificationExchange() {
        return new DirectExchange(NOTIFICATION_EXCHANGE);
    }

    @Bean
    public Queue notificationQueue() {
        return new Queue(NOTIFICATION_QUEUE, true);
    }

    @Bean
    public Binding notificationBinding(Queue notificationQueue, DirectExchange notificationExchange) {
        return BindingBuilder.bind(notificationQueue).to(notificationExchange).with(NOTIFICATION_ROUTING_KEY);
    }

    @Bean
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }

    @Bean
    public DirectExchange registrationExchange() {
        return new DirectExchange(REGISTRATION_EXCHANGE);
    }

    @Bean
    public DirectExchange workshopExchange() {
        return new DirectExchange("workshop.exchange");
    }

    @Bean
    public Queue workshopQueue() {
        return new Queue("workshop.queue", true);
    }

    @Bean
    public Binding workshopBinding(Queue workshopQueue, DirectExchange workshopExchange) {
        return BindingBuilder.bind(workshopQueue).to(workshopExchange).with("workshop.created");
    }

    @Bean
    public Queue registrationQueue() {
        return QueueBuilder.durable(REGISTRATION_QUEUE)
                .withArgument("x-dead-letter-exchange", "registration.dlx")
                .withArgument("x-dead-letter-routing-key", "registration.failed")
                .build();
    }

    @Bean
    public Binding registrationBinding(Queue registrationQueue, DirectExchange registrationExchange) {
        return BindingBuilder.bind(registrationQueue).to(registrationExchange).with(REGISTRATION_ROUTING_KEY);
    }

    @Bean
    public Queue workshopCancelQueue() {
        return new Queue(WORKSHOP_CANCEL_QUEUE, true);
    }

    @Bean
    public Binding workshopCancelBinding(Queue workshopCancelQueue, DirectExchange registrationExchange) {
        return BindingBuilder.bind(workshopCancelQueue).to(registrationExchange).with(WORKSHOP_CANCEL_ROUTING_KEY);
    }
}
