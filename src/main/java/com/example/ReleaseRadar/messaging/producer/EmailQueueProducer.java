package com.example.ReleaseRadar.messaging.producer;

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
    private final IRecipientGroupMemberRepository recipientGroupMemberRepository;
    private final AsyncEmailSender asyncEmailSender;

    // @Scheduled(cron = "0 0 8 * * *")  // prod
    @Scheduled(cron = "0 * * * * *")     // deneme
    public void queueEmailsForSending() {
        log.info("scheduler çalıştı lokal");

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(23, 59, 59);

        List<EmailReminder> reminders = emailReminderRepository
                .findByStatusAndSendDatetimeBetween(ReminderStatus.PENDING, startOfDay, endOfDay);

        for (EmailReminder reminder : reminders) {
            List<String> emails = recipientGroupMemberRepository
                    .findEmailsByEmailReminderId(reminder.getId());

            for (String email : emails) {
                EmailMessageDto dto = EmailMessageDto.builder()
                        .emailReminderId(reminder.getId())
                        .to(email)
                        .subject(reminder.getTemplate().getSubjectTpl())
                        .body(reminder.getTemplate().getBodyHtmlTpl())
                        .build();

                // ASYNC publish
                asyncEmailSender.send(dto);
            }
        }
    }
}
