package com.example.ReleaseRadar.messaging.producer;

import org.junit.jupiter.api.Test;

import com.example.ReleaseRadar.config.RabbitMQConfig;
import com.example.ReleaseRadar.dtos.EmailMessageDto;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.amqp.rabbit.core.RabbitTemplate;


import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
 import static org.mockito.ArgumentMatchers.*;
@ExtendWith(MockitoExtension.class)
class AsyncEmailSenderTest {

    @Mock
    RabbitTemplate rabbitTemplate;

    @Test
    void send_publishesToExchangeWithRouting_andPayload() {
        AsyncEmailSender sender = new AsyncEmailSender(rabbitTemplate);

        EmailMessageDto dto = EmailMessageDto.builder()
                .emailReminderId(42L)
                .to("test@example.com")
                .subject("Hello")
                .body("<b>Hi</b>")
                .build();

        sender.send(dto); // @Async devre dışı (proxy yok) -> senkron çağrı

        ArgumentCaptor<EmailMessageDto> dtoCap = ArgumentCaptor.forClass(EmailMessageDto.class);

        verify(rabbitTemplate).convertAndSend(
                eq(RabbitMQConfig.EMAIL_EXCHANGE),
                eq(RabbitMQConfig.EMAIL_ROUTING),
                dtoCap.capture()
        );

        EmailMessageDto sent = dtoCap.getValue();
        assertEquals(dto.getEmailReminderId(), sent.getEmailReminderId());
        assertEquals(dto.getTo(), sent.getTo());
        assertEquals(dto.getSubject(), sent.getSubject());
        assertEquals(dto.getBody(), sent.getBody());
    }
}