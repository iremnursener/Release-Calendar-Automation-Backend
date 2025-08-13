package com.example.ReleaseRadar.messaging.producer;

import com.example.ReleaseRadar.config.RabbitMQConfig;
import com.example.ReleaseRadar.dtos.EmailMessageDto;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AsyncEmailSender {
    private final RabbitTemplate rabbitTemplate;

    @Async("emailSendExecutor")
//    @Retry(name = "sendEmail")
    public void send(EmailMessageDto dto) {
        rabbitTemplate.convertAndSend(
                RabbitMQConfig.EMAIL_EXCHANGE,
                RabbitMQConfig.EMAIL_ROUTING,
                dto
        );
    }
}