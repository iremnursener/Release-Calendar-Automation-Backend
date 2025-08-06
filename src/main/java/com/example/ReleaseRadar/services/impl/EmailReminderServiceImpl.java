package com.example.ReleaseRadar.services.impl;

import com.example.ReleaseRadar.entities.*;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.*;
import com.example.ReleaseRadar.services.IEmailReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReminderServiceImpl implements IEmailReminderService {

    private final IEventRepository eventRepository;
    private final IEmailTemplateRepository emailTemplateRepository;
    private final IRecipientGroupRepository recipientGroupRepository;
    private final IEmailReminderRepository emailReminderRepository;
    private final IImportBatchEventRepository importBatchEventRepository;

    @Override
    public EmailReminder createReminder(Event event) {

        String templateName = switch (event.getColor().getEventType()) {
            case FREEZE -> "Freeze Template";
            case RELEASE -> "Release Template";
            case BUSY_DAY -> "Busy Day Template";
            case IMPORTANT_PATCH -> "Important Patch Template";
            case IMPORTANT_DRC -> "Important DRC Template";
        };

        EmailTemplate template = emailTemplateRepository.findByName(templateName)
                .orElseThrow(() -> new RuntimeException("Template bulunamadı: " + templateName));

        // 3. Varsayılan grup seç (örnek: All Stakeholders)
        RecipientGroup group = recipientGroupRepository.findByGroupName("All Stakeholders")
                .orElseThrow(() -> new RuntimeException("Recipient group bulunamadı."));

        // 4. Gönderim tarihi = Event tarihinden 5 gün öncesi, saat 08:00
        LocalDateTime sendDateTime = event.getDate()
                .minusDays(5)              // önce 5 gün geriye git
                .atTime(8, 0);              // sonra saat 08:00 olarak ayarla

        // 5. EmailReminder oluştur
        EmailReminder reminder = EmailReminder.builder()
                .event(event)
                .template(template)
                .group(group)
                .sendDatetime(sendDateTime)
                .status(ReminderStatus.PENDING)
                .lastTryAt(null)
                .isActive(true)
                .build();

        return emailReminderRepository.save(reminder);
    }


    public void createRemindersForImportBatch(String batchIdStr) throws IOException {
        try {
            // String UUID'yi Java UUID objesine dönüştür
            UUID batchId = UUID.fromString(batchIdStr);

            // DB'den batchId'ye bağlı tüm ImportBatchEvent kayıtlarını getir
            List<ImportBatchEvent> batchEvents = importBatchEventRepository.findByBatch_Id(batchId);

            if (batchEvents.isEmpty()) {
                System.out.println("Batch ID " + batchId + " için hiçbir event bulunamadı.");
                return;
            }

            // Reminder oluştur
            for (ImportBatchEvent batchEvent : batchEvents) {
                Event event = batchEvent.getEvent();
                if (event != null) {
                    try {
                        createReminder(event);
                    } catch (Exception e) {
                        System.err.println("Event ID " + event.getId() + " için reminder oluşturulamadı: " + e.getMessage());
                    }
                } else {
                    System.err.println("ImportBatchEvent ID " + batchEvent.getId() + " için Event null.");
                }
            }

        } catch (IllegalArgumentException e) {
            System.err.println("Geçersiz UUID formatı: " + batchIdStr);
        }
    }



}
