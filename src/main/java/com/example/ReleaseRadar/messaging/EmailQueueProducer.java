package com.example.ReleaseRadar.messaging;

import com.example.ReleaseRadar.dtos.EmailMessageDto;
import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.IEmailReminderRepository;
import com.example.ReleaseRadar.repositories.IRecipientGroupMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailQueueProducer {

    private final IEmailReminderRepository emailReminderRepository;
    private final RabbitTemplate rabbitTemplate;
    private final IRecipientGroupMemberRepository recipientGroupMemberRepository;

    // Her sabah 08:00'de çalışır
    // @Scheduled(cron = "0 0 8 * * *")
    @Scheduled(cron = "0 * * * * *") // her dakika başında-DENEME

    public void queueEmailsForSending() {

        log.info("scheduler çalıştı" );

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();         // 00:00  KONTROL
        LocalDateTime endOfDay = today.atTime(23, 59, 59);       // 23:59:59  KONTROL
        //LocalDateTime todayAtEight = today.atTime(8, 0);

//        List<EmailReminder> reminders = emailReminderRepository
//                .findByStatusAndSendDatetime(ReminderStatus.PENDING, LocalDateTime.now()); //normalde saat 8 de olacak yani todayAtEight

        List<EmailReminder> reminders = emailReminderRepository  //GÜN İÇİNDE GÖNDERİLMESİ GEREKEN MAİL VARSA ALIR
                .findByStatusAndSendDatetimeBetween(ReminderStatus.PENDING, startOfDay, endOfDay);

        for (EmailReminder reminder : reminders) {

            List<String> emailAddresses =
                    recipientGroupMemberRepository.findEmailsByEmailReminderId(reminder.getId());

            for (String email : emailAddresses) {
                EmailMessageDto dto = EmailMessageDto.builder()
                        .emailReminderId(reminder.getId())
                        .to(email)
                        .subject(reminder.getTemplate().getSubjectTpl())
                        .body(reminder.getTemplate().getBodyHtmlTpl())
                        .build();

                rabbitTemplate.convertAndSend("email-send-queue", dto);
            }
    }
}}
