package com.example.ReleaseRadar.messaging.consumer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ReleaseRadar.dtos.EmailMessageDto;
import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.entities.Event;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.IEmailReminderRepository;
import jakarta.mail.Session;
import jakarta.mail.internet.MimeMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueConsumerTest {

    @Mock private JavaMailSender mailSender;
    @Mock private IEmailReminderRepository emailReminderRepository;

    private EmailQueueConsumer consumer;

    @BeforeEach
    void setUp() {
        consumer = new EmailQueueConsumer(mailSender, emailReminderRepository);
    }

    private EmailReminder reminderWithEvent(long id, LocalDate date) {
        Event ev = Event.builder().date(date).build(); // Senin entity'nde builder yoksa setter kullan
        return EmailReminder.builder()
                .id(id)
                .status(ReminderStatus.PENDING)
                .event(ev)
                .build();
    }

    @Test
    void consume_success_sendsMail_andMarksSent() throws Exception {
        // Arrange
        EmailMessageDto msg = EmailMessageDto.builder()
                .emailReminderId(1L)
                .to("user@example.com")
                .subject("Release {{date}}")
                .body("<b>{{date}}</b>")
                .build();

        EmailReminder rem = reminderWithEvent(1L, LocalDate.of(2025, 8, 30));
        when(emailReminderRepository.findById(1L)).thenReturn(Optional.of(rem));

        // gerçek MimeMessage; gönderimi ise mock'luyoruz
        MimeMessage realMime = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(realMime);

        // Act
        consumer.consume(msg);

        // Assert – mail gönderildi
        ArgumentCaptor<MimeMessage> mimeCap = ArgumentCaptor.forClass(MimeMessage.class);
        verify(mailSender).send(mimeCap.capture());

        // Konu içinde tarih token'ı doğru basılmış mı?
        assertEquals("Release 30.08.2025", mimeCap.getValue().getSubject());

        // DB güncellemesi (SENT + lastTryAt) yapıldı mı?
        ArgumentCaptor<EmailReminder> remCap = ArgumentCaptor.forClass(EmailReminder.class);
        verify(emailReminderRepository, atLeastOnce()).save(remCap.capture());
        EmailReminder saved = remCap.getValue();
        assertEquals(ReminderStatus.SENT, saved.getStatus());
        assertNotNull(saved.getLastTryAt());
    }

    @Test
    void consume_mailSenderThrows_setsLastTry_thenThrowsRuntime() throws Exception {
        // Arrange
        EmailMessageDto msg = EmailMessageDto.builder()
                .emailReminderId(5L)
                .to("user@example.com")
                .subject("S {{date}}")
                .body("B {{date}}")
                .build();

        EmailReminder rem = reminderWithEvent(5L, LocalDate.of(2025, 9, 1));
        when(emailReminderRepository.findById(5L)).thenReturn(Optional.of(rem));

        MimeMessage realMime = new MimeMessage((Session) null);
        when(mailSender.createMimeMessage()).thenReturn(realMime);
        doThrow(new RuntimeException("SMTP down")).when(mailSender).send(any(MimeMessage.class));

        // Act + Assert – RuntimeException beklenir
        RuntimeException ex = assertThrows(RuntimeException.class, () -> consumer.consume(msg));
        assertTrue(ex.getMessage().contains("Email send failed"));

        // Catch bloğunda lastTryAt güncellenip save edilmeliydi
        ArgumentCaptor<EmailReminder> cap = ArgumentCaptor.forClass(EmailReminder.class);
        verify(emailReminderRepository).save(cap.capture());
        assertNotNull(cap.getValue().getLastTryAt());

        // SENT'e çekilmemiş olmalı
        assertNotEquals(ReminderStatus.SENT, rem.getStatus());
    }

    @Test
    void recover_marksFailed_andRejectsToDLQ() {
        // Arrange
        EmailMessageDto msg = EmailMessageDto.builder()
                .emailReminderId(9L)
                .build();

        EmailReminder rem = reminderWithEvent(9L, LocalDate.of(2025, 10, 10));
        when(emailReminderRepository.findById(9L)).thenReturn(Optional.of(rem));

        // Act + Assert – Recover sonunda DLQ'ya reject eden exception fırlatır
        AmqpRejectAndDontRequeueException thrown =
                assertThrows(AmqpRejectAndDontRequeueException.class,
                        () -> consumer.recover(new RuntimeException("after retries"), msg));

        assertTrue(thrown.getMessage().contains("Retries exhausted"));

        // FAILED ve lastTryAt set edilip kaydedilmiş olmalı
        ArgumentCaptor<EmailReminder> cap = ArgumentCaptor.forClass(EmailReminder.class);
        verify(emailReminderRepository).save(cap.capture());
        EmailReminder saved = cap.getValue();
        assertEquals(ReminderStatus.FAILED, saved.getStatus());
        assertNotNull(saved.getLastTryAt());
    }

    @Test
    void consume_whenReminderNotFound_throwsNoSuchElement() {
        EmailMessageDto msg = EmailMessageDto.builder().emailReminderId(111L).build();
        when(emailReminderRepository.findById(111L)).thenReturn(Optional.empty());

        assertThrows(java.util.NoSuchElementException.class, () -> consumer.consume(msg));
        verifyNoInteractions(mailSender);
    }
}