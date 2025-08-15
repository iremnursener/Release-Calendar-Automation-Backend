package com.example.ReleaseRadar.services.impl;

import com.example.ReleaseRadar.entities.*;
import com.example.ReleaseRadar.enums.EventType;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.example.ReleaseRadar.exceptions.RecipientGroupNotFoundException;
import com.example.ReleaseRadar.exceptions.TemplateNotFoundException;
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
/**
 * {@link EmailReminder} nesnelerinin oluşturulmasından sorumlu servis.
 *
 * <p><b>Temel görevler:</b></p>
 * <ul>
 *     <li>Event tipine uygun e-posta şablonunu belirlemek</li>
 *     <li>Varsayılan alıcı grubunu yüklemek</li>
 *     <li>Etkinlik tarihine göre gönderim zamanını hesaplamak</li>
 *     <li>Belirli bir ImportBatch için tüm etkinliklere hatırlatıcı oluşturmak</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EmailReminderServiceImpl implements IEmailReminderService {

    private final IEventRepository eventRepository;
    private final IEmailTemplateRepository emailTemplateRepository;
    private final IRecipientGroupRepository recipientGroupRepository;
    private final IEmailReminderRepository emailReminderRepository;
    private final IImportBatchEventRepository importBatchEventRepository;

    /**
     * Verilen {@link Event} için bir {@link EmailReminder} oluşturur ve kaydeder.
     *
     * @param event Hatırlatıcı oluşturulacak etkinlik
     * @return Kaydedilmiş {@link EmailReminder}
     * @throws TemplateNotFoundException İlgili e-posta şablonu bulunamazsa
     * @throws RecipientGroupNotFoundException Varsayılan alıcı grubu bulunamazsa
     */
    private EmailReminder createReminder(Event event) {
        String templateName = resolveTemplateName(event.getColor().getEventType());
        EmailTemplate template = getTemplateByName(templateName);
        RecipientGroup group = getDefaultRecipientGroup();
        LocalDateTime sendDateTime = calculateSendDateTime(event.getDate());
        EmailReminder reminder = buildReminder(event, template, group, sendDateTime);
        return emailReminderRepository.save(reminder);
    }
    /**
     * Belirtilen batchId’ye ait tüm {@link ImportBatchEvent} kayıtlarını alır
     * ve her biri için {@link EmailReminder} oluşturur.
     *
     * @param batchIdStr İşlenecek batch ID’si (String formatında UUID)
     * @throws IOException IO ile ilgili beklenmeyen bir hata oluşursa
     */
    public void createRemindersForImportBatch(String batchIdStr) throws IOException {
        try {
            UUID batchId = parseBatchId(batchIdStr);
            List<ImportBatchEvent> batchEvents = fetchBatchEvents(batchId);

            if (batchEvents.isEmpty()) {
                log.info("Batch ID " + batchId + " için hiçbir event bulunamadı.");
                return;
            }

            processBatchEvents(batchEvents);

        } catch (IllegalArgumentException e) {
            log.error("Geçersiz UUID formatı: " + batchIdStr);
        }
    }

    // ===================== Helpers =====================
    /**
     * Event tipine göre kullanılacak şablon adını döndürür.
     *
     * @param eventType Etkinlik tipi
     * @return Şablon adı
     */
    private String resolveTemplateName(EventType eventType) {
        return switch (eventType) {
            case FREEZE -> "Freeze Template";
            case RELEASE -> "Release Template";
            case BUSY_DAY -> "Busy Day Template";
            case IMPORTANT_PATCH -> "Important Patch Template";
            case IMPORTANT_DRC -> "Important DRC Template";
        };
    }
    /**
     * Şablon adından {@link EmailTemplate} yükler.
     *
     * @param templateName Şablon adı
     * @return Bulunan {@link EmailTemplate}
     * @throws TemplateNotFoundException Şablon bulunamazsa
     */
    private EmailTemplate getTemplateByName(String templateName) {
        return emailTemplateRepository.findByName(templateName)
                .orElseThrow(() -> new TemplateNotFoundException("Template bulunamadı: " + templateName));
    }
    /**
     * Varsayılan alıcı grubunu yükler.
     *
     * @return Varsayılan {@link RecipientGroup}
     * @throws RecipientGroupNotFoundException Grup bulunamazsa
     */
    private RecipientGroup getDefaultRecipientGroup() {
        return recipientGroupRepository.findByGroupName("All Stakeholders")
                .orElseThrow(() -> new RecipientGroupNotFoundException("Recipient group bulunamadı."));
    }
    /**
     * Etkinlik tarihinden 5 gün önce, saat 08:00 olarak gönderim zamanını hesaplar.
     *
     * @param eventDate Etkinlik tarihi
     * @return Gönderim zamanı
     */
    private LocalDateTime calculateSendDateTime(LocalDate eventDate) {
        return eventDate.minusDays(5).atTime(8, 0);
    }
    /**
     * Verilen parametrelerle {@link EmailReminder} nesnesi oluşturur.
     *
     * @param event         İlgili etkinlik
     * @param template      Kullanılacak e-posta şablonu
     * @param group         Alıcı grubu
     * @param sendDateTime  Gönderim zamanı
     * @return Kurulmuş (henüz kaydedilmemiş) {@link EmailReminder}
     */
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
    /**
     * String formatındaki batch ID’yi {@link UUID}’ye çevirir.
     *
     * @param batchIdStr Batch ID (String)
     * @return {@link UUID} formatındaki batch ID
     * @throws IllegalArgumentException Format geçersizse
     */
    private UUID parseBatchId(String batchIdStr) {
        return UUID.fromString(batchIdStr);
    }
    /**
     * Verilen batch ID’ye ait tüm {@link ImportBatchEvent} kayıtlarını döndürür.
     *
     * @param batchId Batch kimliği
     * @return Batch’e ait etkinliklerin listesi
     */
    private List<ImportBatchEvent> fetchBatchEvents(UUID batchId) {
        return importBatchEventRepository.findByBatch_Id(batchId);
    }
    /**
     * Liste halindeki {@link ImportBatchEvent} kayıtlarını işleyerek
     * her biri için hatırlatıcı oluşturur.
     *
     * @param batchEvents İşlenecek etkinlikler
     */
    private void processBatchEvents(List<ImportBatchEvent> batchEvents) {
        for (ImportBatchEvent batchEvent : batchEvents) {
            handleSingleBatchEvent(batchEvent);
        }
    }
    /**
     * Tek bir {@link ImportBatchEvent} için hatırlatıcı oluşturma sürecini yönetir.
     *
     * @param batchEvent İşlenecek {@link ImportBatchEvent}
     */
    private void handleSingleBatchEvent(ImportBatchEvent batchEvent) {
        Event event = batchEvent.getEvent();
        if (event != null) {
            try {
                createReminder(event);
            } catch (Exception e) {
                log.error("Event ID " + event.getId() + " için reminder oluşturulamadı: " + e.getMessage());
            }
        } else {
            log.error("ImportBatchEvent ID " + batchEvent.getId() + " için Event null.");
        }
    }
}
