package com.example.ReleaseRadar.config;

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mockito;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.Binding.DestinationType;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerEndpoint;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {
        RabbitMQConfig.class,
        RabbitMQConfigTest.TestConfig.class // sahte ConnectionFactory sağlar
})
class RabbitMQConfigTest {

    static class TestConfig {
        @Bean
        ConnectionFactory connectionFactory() {
            return Mockito.mock(ConnectionFactory.class);
        }
    }

    @Autowired
    ApplicationContext ctx;

    @Test
    void exchanges_and_queues_and_bindings_exist_with_expected_names() {
        DirectExchange ex = ctx.getBean("emailExchange", DirectExchange.class);
        DirectExchange dlx = ctx.getBean("emailDlx", DirectExchange.class);
        Queue q = ctx.getBean("emailQueue", Queue.class);
        Queue dlq = ctx.getBean("emailDlq", Queue.class);
        Binding bind = ctx.getBean("emailBinding", Binding.class);
        Binding dlqBind = ctx.getBean("emailDlqBinding", Binding.class);

        // Exchange/Queue adları
        assertEquals(RabbitMQConfig.EMAIL_EXCHANGE, ex.getName());
        assertEquals(RabbitMQConfig.EMAIL_DLX, dlx.getName());
        assertEquals(RabbitMQConfig.EMAIL_QUEUE, q.getName());
        assertEquals(RabbitMQConfig.EMAIL_DLQ, dlq.getName());

        // Binding yönleri ve routing key
        assertEquals(RabbitMQConfig.EMAIL_QUEUE, bind.getDestination());
        assertEquals(DestinationType.QUEUE, bind.getDestinationType());
        assertEquals(RabbitMQConfig.EMAIL_EXCHANGE, bind.getExchange());
        assertEquals(RabbitMQConfig.EMAIL_ROUTING, bind.getRoutingKey());

        assertEquals(RabbitMQConfig.EMAIL_DLQ, dlqBind.getDestination());
        assertEquals(DestinationType.QUEUE, dlqBind.getDestinationType());
        assertEquals(RabbitMQConfig.EMAIL_DLX, dlqBind.getExchange());
        assertEquals(RabbitMQConfig.EMAIL_DLQ_ROUT, dlqBind.getRoutingKey());
    }

    @Test
    void emailQueue_has_dlx_arguments_and_is_durable() {
        Queue q = ctx.getBean("emailQueue", Queue.class);

        assertTrue(q.isDurable());
        assertFalse(q.isExclusive());
        assertFalse(q.isAutoDelete());

        Map<String, Object> args = q.getArguments();
        assertEquals(RabbitMQConfig.EMAIL_DLX, args.get("x-dead-letter-exchange"));
        assertEquals(RabbitMQConfig.EMAIL_DLQ_ROUT, args.get("x-dead-letter-routing-key"));
    }

    @Test
    void messageConverter_is_jackson_and_shared() {
        MessageConverter mc = ctx.getBean("messageConverter", MessageConverter.class);
        assertTrue(mc instanceof Jackson2JsonMessageConverter);

        RabbitTemplate tpl = ctx.getBean(RabbitTemplate.class);
        // RabbitTemplate’te kullanılan converter aynı instance olmalı
        assertSame(mc, tpl.getMessageConverter());
    }

    @Test
    void listenerFactory_settings_applied_via_reflection() {
        SimpleRabbitListenerContainerFactory f =
                ctx.getBean("rabbitListenerContainerFactory", SimpleRabbitListenerContainerFactory.class);

        // Test endpoint
        SimpleRabbitListenerEndpoint endpoint = new SimpleRabbitListenerEndpoint();
        endpoint.setId("test");
        endpoint.setQueueNames("dummy");
        endpoint.setMessageListener((MessageListener) m -> {});

        // Factory'den container üret
        SimpleMessageListenerContainer container = f.createListenerContainer(endpoint);

        // ---- PUBLIC GETTER YOKSA REFLECTION KULLAN ----
        Boolean defaultRequeue = (Boolean) ReflectionTestUtils
                .getField(container, "defaultRequeueRejected");
        assertNotNull(defaultRequeue);
        assertFalse(defaultRequeue); // DLQ akışı için önemli

        Integer conc = (Integer) ReflectionTestUtils
                .getField(container, "concurrentConsumers");
        Integer maxConc = (Integer) ReflectionTestUtils
                .getField(container, "maxConcurrentConsumers");
        assertEquals(2, conc);
        assertEquals(8, maxConc);

        // Factory'nin converter'ı (getter yoksa) reflection ile doğrula
        Object factoryMc = ReflectionTestUtils.getField(f, "messageConverter");
        MessageConverter mcBean = ctx.getBean("messageConverter", MessageConverter.class);
        assertTrue(mcBean instanceof Jackson2JsonMessageConverter);
        assertSame(mcBean, factoryMc);
    }
}
