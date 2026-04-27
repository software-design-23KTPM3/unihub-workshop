package com.unihub.backend.core.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String REGISTRATION_EXCHANGE = "registration.exchange";
    public static final String REGISTRATION_QUEUE = "registration.queue";
    public static final String REGISTRATION_ROUTING_KEY = "registration.created";

    public static final String WORKSHOP_CANCEL_QUEUE = "workshop.cancel.queue";
    public static final String WORKSHOP_CANCEL_ROUTING_KEY = "workshop.cancelled";

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
