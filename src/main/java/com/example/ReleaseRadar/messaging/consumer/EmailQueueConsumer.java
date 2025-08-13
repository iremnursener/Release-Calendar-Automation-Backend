package com.example.ReleaseRadar.messaging.consumer;

import com.example.ReleaseRadar.config.RabbitMQConfig;
import com.example.ReleaseRadar.dtos.EmailMessageDto;
import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.IEmailReminderRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.amqp.AmqpRejectAndDontRequeueException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

@AllArgsConstructor
@Service
public class EmailQueueConsumer {

    private final JavaMailSender mailSender;
    private final IEmailReminderRepository emailReminderRepository;

    @RabbitListener(
            queues = RabbitMQConfig.EMAIL_QUEUE,
            containerFactory = "rabbitListenerContainerFactory"
    )
    @Retryable(
            // Hangi hatalarda retry edilsin? (geniş tuttuk; istersen sadece MailException vs.)
            include = { RuntimeException.class },
            maxAttempts = 3,
            backoff = @Backoff(delay = 1000, multiplier = 2.0, maxDelay = 10000)
    )


    public void consume(EmailMessageDto message) {  // retry attemp=3 eklenmedi
        EmailReminder reminder = emailReminderRepository.findById(message.getEmailReminderId())
                .orElseThrow();

        try {
            String formattedDate = reminder.getEvent().getDate()
                    .format(DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String subject = message.getSubject().replace("{{date}}", formattedDate);
            String body    = message.getBody().replace("{{date}}", formattedDate);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(message.getTo());
            helper.setSubject(subject);
            helper.setText(body, true);

            mailSender.send(mimeMessage);

            reminder.setStatus(ReminderStatus.SENT);
            reminder.setLastTryAt(LocalDateTime.now());
            emailReminderRepository.save(reminder);
        } catch (Exception e) {
            // DB'ye FAILED yaz, sonra requeue etme -> DLQ'ya düşsün
            reminder.setLastTryAt(LocalDateTime.now());
            emailReminderRepository.save(reminder);
            throw new RuntimeException("Email send failed; will be retried", e);
        }

    }
    @Recover
    public void recover(RuntimeException e, EmailMessageDto message) {
        // DB'ye FAILED yaz
        if (message != null && message.getEmailReminderId() != null) {
            emailReminderRepository.findById(message.getEmailReminderId()).ifPresent(rem -> {
                rem.setStatus(ReminderStatus.FAILED);
                rem.setLastTryAt(LocalDateTime.now());
                emailReminderRepository.save(rem);
            });
        }

        // Requeue etme -> DLQ'ya gitsin (factory'de defaultRequeueRejected=false)
        throw new AmqpRejectAndDontRequeueException("Retries exhausted; rejecting to DLQ", e);
    }


}
