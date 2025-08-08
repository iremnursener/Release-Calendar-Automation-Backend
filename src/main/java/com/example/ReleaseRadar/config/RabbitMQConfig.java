package com.example.ReleaseRadar.config;


import org.springframework.amqp.core.Queue;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Collections;

@Configuration
public class RabbitMQConfig {


    //direct exchange oluştur ,routing key olcak
    @Bean
    public Queue emailQueue() {
        return new Queue("email-send-queue", true);
    }

    @Bean
    public MessageConverter messageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(Collections.singletonList("com.example.ReleaseRadar.dtos.*"));
        return converter;
    }
}