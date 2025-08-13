package com.example.ReleaseRadar.config;

import com.example.ReleaseRadar.dtos.EmailMessageDto;
import com.example.ReleaseRadar.repositories.IEmailReminderRepository;
import org.aopalliance.aop.Advice;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.DefaultJackson2JavaTypeMapper;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class RabbitMQConfig {


    // ---- Names ----
    public static final String EMAIL_EXCHANGE = "email.direct";
    public static final String EMAIL_ROUTING  = "email.send";

    public static final String EMAIL_QUEUE    = "email-send-queue.v2";
    public static final String EMAIL_DLX      = "email.dlx";
    public static final String EMAIL_DLQ      = "email-send-queue.dlq";
    public static final String EMAIL_DLQ_ROUT = "email.send.dlq";

    // ---- Exchanges ----
    @Bean
    public DirectExchange emailExchange() {
        return new DirectExchange(EMAIL_EXCHANGE, true, false);
    }

    @Bean
    public DirectExchange emailDlx() {
        return new DirectExchange(EMAIL_DLX, true, false);
    }

    // ---- Queues ----
    @Bean
    public Queue emailQueue() {
        Map<String, Object> args = new HashMap<>();
        // Hata olursa DLX'e gönder
        args.put("x-dead-letter-exchange", EMAIL_DLX);
        args.put("x-dead-letter-routing-key", EMAIL_DLQ_ROUT);
        return new Queue(EMAIL_QUEUE, true, false, false, args);
    }

    @Bean
    public Queue emailDlq() {
        return new Queue(EMAIL_DLQ, true);
    }

    // ---- Bindings ----
    @Bean
    public Binding emailBinding() {
        return BindingBuilder.bind(emailQueue()).to(emailExchange()).with(EMAIL_ROUTING);
    }

    @Bean
    public Binding emailDlqBinding() {
        return BindingBuilder.bind(emailDlq()).to(emailDlx()).with(EMAIL_DLQ_ROUT);
    }

    // ---- Message converter ----
    @Bean
    public MessageConverter messageConverter() {
        // JSON öneririm (DTO'lar için daha güvenli)
        Jackson2JsonMessageConverter conv = new Jackson2JsonMessageConverter();
        conv.setClassMapper(new DefaultJackson2JavaTypeMapper());
        return conv;
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory cf, MessageConverter mc) {
        RabbitTemplate t = new RabbitTemplate(cf);
        t.setMessageConverter(mc);
        return t;
    }


    // Listener ayarları: requeue=false => hata DLQ'ya düşer
    @Bean
    public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
            ConnectionFactory cf, MessageConverter mc) {
        SimpleRabbitListenerContainerFactory f = new SimpleRabbitListenerContainerFactory();
        f.setConnectionFactory(cf);
        f.setMessageConverter(mc);
        f.setDefaultRequeueRejected(false);
        f.setConcurrentConsumers(2);
        f.setMaxConcurrentConsumers(8);
        return f;
    }
}