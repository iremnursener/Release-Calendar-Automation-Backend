package com.example.ReleaseRadar.messaging.producer;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ReleaseRadar.dtos.EmailMessageDto;
import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.entities.EmailTemplate;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.IEmailReminderRepository;
import com.example.ReleaseRadar.repositories.IRecipientGroupMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailQueueProducerTest {

    @Mock private IEmailReminderRepository emailReminderRepository;
    @Mock private IRecipientGroupMemberRepository recipientGroupMemberRepository;
    @Mock private AsyncEmailSender asyncEmailSender;

    private EmailQueueProducer producer;

    @BeforeEach
    void setUp() {
        producer = new EmailQueueProducer(
                emailReminderRepository,
                recipientGroupMemberRepository,
                asyncEmailSender
        );
    }

    private EmailReminder reminder(long id, String subj, String body, LocalDateTime when) {
        EmailTemplate tpl = EmailTemplate.builder()
                .subjectTpl(subj)
                .bodyHtmlTpl(body)
                .build();

        return EmailReminder.builder()
                .id(id)
                .template(tpl)
                .status(ReminderStatus.PENDING)
                .sendDatetime(when)
                .build();
    }

    @Test
    void queueEmailsForSending_sendsForEachRecipientOfEachReminder() {
        // arrange
        LocalDateTime now = LocalDateTime.now();
        EmailReminder r1 = reminder(1L, "S1", "<p>B1</p>", now);
        EmailReminder r2 = reminder(2L, "S2", "<p>B2</p>", now.plusHours(1));

        when(emailReminderRepository.findByStatusAndSendDatetimeBetween(
                eq(ReminderStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(r1, r2));

        when(recipientGroupMemberRepository.findEmailsByEmailReminderId(1L))
                .thenReturn(List.of("a@ex.com", "b@ex.com"));
        when(recipientGroupMemberRepository.findEmailsByEmailReminderId(2L))
                .thenReturn(List.of("c@ex.com"));

        // act
        producer.queueEmailsForSending();

        // assert: toplam 3 gönderi (2 + 1)
        ArgumentCaptor<EmailMessageDto> dtoCap = ArgumentCaptor.forClass(EmailMessageDto.class);
        verify(asyncEmailSender, times(3)).send(dtoCap.capture());

        List<EmailMessageDto> all = dtoCap.getAllValues();
        // r1 için 2 adet
        long r1Count = all.stream().filter(d -> d.getEmailReminderId().equals(1L)).count();
        long r2Count = all.stream().filter(d -> d.getEmailReminderId().equals(2L)).count();
        assertEquals(2, r1Count);
        assertEquals(1, r2Count);

        // konu/gövde doğru kopyalanmış mı?
        EmailMessageDto anyR1 = all.stream().filter(d -> d.getEmailReminderId().equals(1L)).findFirst().orElseThrow();
        assertEquals("S1", anyR1.getSubject());
        assertEquals("<p>B1</p>", anyR1.getBody());
        EmailMessageDto anyR2 = all.stream().filter(d -> d.getEmailReminderId().equals(2L)).findFirst().orElseThrow();
        assertEquals("S2", anyR2.getSubject());
        assertEquals("<p>B2</p>", anyR2.getBody());

        // repository çağrısı PENDING ve bugünün aralığıyla yapılmalı
        verify(emailReminderRepository).findByStatusAndSendDatetimeBetween(
                eq(ReminderStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class));

        // her reminder için recipient sorgusu yapılmalı
        verify(recipientGroupMemberRepository).findEmailsByEmailReminderId(1L);
        verify(recipientGroupMemberRepository).findEmailsByEmailReminderId(2L);

        verifyNoMoreInteractions(asyncEmailSender, emailReminderRepository, recipientGroupMemberRepository);
    }

    @Test
    void queueEmailsForSending_whenNoReminders_doesNothing() {
        when(emailReminderRepository.findByStatusAndSendDatetimeBetween(
                eq(ReminderStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of());

        producer.queueEmailsForSending();

        verifyNoInteractions(recipientGroupMemberRepository, asyncEmailSender);
    }

    @Test
    void queueEmailsForSending_whenNoRecipients_doesNotSend() {
        LocalDateTime now = LocalDate.now().atTime(8, 0);
        EmailReminder r = reminder(10L, "S", "<b>B</b>", now);

        when(emailReminderRepository.findByStatusAndSendDatetimeBetween(
                eq(ReminderStatus.PENDING), any(LocalDateTime.class), any(LocalDateTime.class)))
                .thenReturn(List.of(r));
        when(recipientGroupMemberRepository.findEmailsByEmailReminderId(10L))
                .thenReturn(List.of()); // hiçbir alıcı yok

        producer.queueEmailsForSending();

        verify(recipientGroupMemberRepository).findEmailsByEmailReminderId(10L);
        verifyNoInteractions(asyncEmailSender);
    }
}