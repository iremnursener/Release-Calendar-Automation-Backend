package com.example.ReleaseRadar.messaging;

import com.example.ReleaseRadar.dtos.EmailMessageDto;
import com.example.ReleaseRadar.entities.EmailReminder;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.IEmailReminderRepository;
import jakarta.mail.internet.MimeMessage;
import lombok.AllArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
@AllArgsConstructor
@Service
public class EmailQueueConsumer {

    private final JavaMailSender mailSender;
    private final IEmailReminderRepository emailReminderRepository;

    @RabbitListener(queues = "email-send-queue")
    public void consume(EmailMessageDto message) {
        EmailReminder reminder = emailReminderRepository.findById(message.getEmailReminderId())
                .orElseThrow();

        //TO do: hata alınırsa dead letter queueya atılacak count 3 olcak
        try {
            // Event tarihini al ve formatla
            String formattedDate = reminder.getEvent().getDate()
                    .format(java.time.format.DateTimeFormatter.ofPattern("dd.MM.yyyy"));

            String subject = message.getSubject().replace("{{date}}", formattedDate);
            String body = message.getBody().replace("{{date}}", formattedDate);

            MimeMessage mimeMessage = mailSender.createMimeMessage();
            MimeMessageHelper helper = new MimeMessageHelper(mimeMessage, true);
            helper.setTo(message.getTo());
            helper.setSubject(subject);
            helper.setText(body, true); // HTML body

            mailSender.send(mimeMessage);

            reminder.setStatus(ReminderStatus.SENT);
        } catch (Exception e) {
            reminder.setStatus(ReminderStatus.FAILED);
        }

        reminder.setLastTryAt(LocalDateTime.now());
        emailReminderRepository.save(reminder);
    }
}
