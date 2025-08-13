package com.example.ReleaseRadar.services.impl;

import com.example.ReleaseRadar.entities.*;
import com.example.ReleaseRadar.enums.EventType;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.repositories.*;
import com.example.ReleaseRadar.services.IEmailReminderService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDate;
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


    private EmailReminder createReminder(Event event) {
        String templateName = resolveTemplateName(event.getColor().getEventType());
        EmailTemplate template = getTemplateByName(templateName);
        RecipientGroup group = getDefaultRecipientGroup();
        LocalDateTime sendDateTime = calculateSendDateTime(event.getDate());
        EmailReminder reminder = buildReminder(event, template, group, sendDateTime);
        return emailReminderRepository.save(reminder);
    }

    public void createRemindersForImportBatch(String batchIdStr) throws IOException {
        try {
            UUID batchId = parseBatchId(batchIdStr);
            List<ImportBatchEvent> batchEvents = fetchBatchEvents(batchId);

            if (batchEvents.isEmpty()) {
                System.out.println("Batch ID " + batchId + " için hiçbir event bulunamadı.");
                return;
            }

            processBatchEvents(batchEvents);

        } catch (IllegalArgumentException e) {
            System.err.println("Geçersiz UUID formatı: " + batchIdStr);
        }
    }

    // ===================== Helpers =====================

    private String resolveTemplateName(EventType eventType) {
        return switch (eventType) {
            case FREEZE -> "Freeze Template";
            case RELEASE -> "Release Template";
            case BUSY_DAY -> "Busy Day Template";
            case IMPORTANT_PATCH -> "Important Patch Template";
            case IMPORTANT_DRC -> "Important DRC Template";
        };
    }

    private EmailTemplate getTemplateByName(String templateName) {
        return emailTemplateRepository.findByName(templateName)
                .orElseThrow(() -> new RuntimeException("Template bulunamadı: " + templateName));
    }

    private RecipientGroup getDefaultRecipientGroup() {
        return recipientGroupRepository.findByGroupName("All Stakeholders")
                .orElseThrow(() -> new RuntimeException("Recipient group bulunamadı."));
    }

    private LocalDateTime calculateSendDateTime(LocalDate eventDate) {
        return eventDate.minusDays(5).atTime(8, 0);
    }

    private EmailReminder buildReminder(Event event,
                                        EmailTemplate template,
                                        RecipientGroup group,
                                        LocalDateTime sendDateTime) {
        return EmailReminder.builder()
                .event(event)
                .template(template)
                .group(group)
                .sendDatetime(sendDateTime)
                .status(ReminderStatus.PENDING)
                .lastTryAt(null)
                .isActive(true)
                .build();
    }

    private UUID parseBatchId(String batchIdStr) {
        return UUID.fromString(batchIdStr);
    }

    private List<ImportBatchEvent> fetchBatchEvents(UUID batchId) {
        return importBatchEventRepository.findByBatch_Id(batchId);
    }

    private void processBatchEvents(List<ImportBatchEvent> batchEvents) {
        for (ImportBatchEvent batchEvent : batchEvents) {
            handleSingleBatchEvent(batchEvent);
        }
    }

    private void handleSingleBatchEvent(ImportBatchEvent batchEvent) {
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
}
