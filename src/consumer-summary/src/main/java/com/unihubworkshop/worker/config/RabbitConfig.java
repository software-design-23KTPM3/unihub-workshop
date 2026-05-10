package com.unihubworkshop.worker.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;

@Configuration
public class RabbitConfig {

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
    public Jackson2JsonMessageConverter jsonMessageConverter() {
        Jackson2JsonMessageConverter converter = new Jackson2JsonMessageConverter();

        DefaultJackson2JavaTypeMapper typeMapper = new DefaultJackson2JavaTypeMapper();
        typeMapper.setTrustedPackages("*");

        converter.setJavaTypeMapper(typeMapper);
        return converter;
    }
}
