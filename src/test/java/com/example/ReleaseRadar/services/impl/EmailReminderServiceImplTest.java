package com.example.ReleaseRadar.services.impl;

import static org.junit.jupiter.api.Assertions.*;
import com.example.ReleaseRadar.entities.*;
import com.example.ReleaseRadar.enums.EventType;
import com.example.ReleaseRadar.repositories.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class EmailReminderServiceImplTest {

    @Mock private IEventRepository eventRepository; // bu sınıfta direkt kullanılmıyor ama mock dursun
    @Mock private IEmailTemplateRepository emailTemplateRepository;
    @Mock private IRecipientGroupRepository recipientGroupRepository;
    @Mock private IEmailReminderRepository emailReminderRepository;
    @Mock private IImportBatchEventRepository importBatchEventRepository;

    @InjectMocks
    private EmailReminderServiceImpl service;

    @Test
    void shouldCreateReminders_forAllBatchEvents_withCorrectSendTimes() throws Exception {
        // Arrange
        UUID batchId = UUID.randomUUID();
        String batchIdStr = batchId.toString();



        Event e1 = event(EventType.FREEZE, LocalDate.of(2025, 2, 10)); // send: 2025-02-05 08:00
        Event e2 = event(EventType.RELEASE, LocalDate.of(2025, 3, 5));  // send: 2025-02-28 08:00

        when(importBatchEventRepository.findByBatch_Id(batchId)).thenReturn(List.of(
                ImportBatchEvent.builder().event(e1).build(),
                ImportBatchEvent.builder().event(e2).build()
        ));

        when(emailTemplateRepository.findByName("Freeze Template"))
                .thenReturn(Optional.of(EmailTemplate.builder().id(1L).name("Freeze Template").build()));
        when(emailTemplateRepository.findByName("Release Template"))
                .thenReturn(Optional.of(EmailTemplate.builder().id(2L).name("Release Template").build()));

        when(recipientGroupRepository.findByGroupName("All Stakeholders"))
                .thenReturn(Optional.of(RecipientGroup.builder().id(1L).groupName("All Stakeholders").build()));

        when(emailReminderRepository.save(any(EmailReminder.class))).thenAnswer(inv -> {
            EmailReminder r = inv.getArgument(0);
            r.setId(100L);
            return r;
        });

        // Act
        service.createRemindersForImportBatch(batchIdStr);

        // Assert
        ArgumentCaptor<EmailReminder> captor = ArgumentCaptor.forClass(EmailReminder.class);
        verify(emailReminderRepository, times(2)).save(captor.capture());

        List<EmailReminder> saved = captor.getAllValues();
        assertThat(saved).hasSize(2);

        // sendDatetime kontrolü (–5 gün, 08:00)
        LocalDateTime expected1 = LocalDateTime.of(2025, 2, 5, 8, 0);
        LocalDateTime expected2 = LocalDateTime.of(2025, 2, 28, 8, 0);

        assertThat(saved).anySatisfy(r ->
                assertThat(r.getEvent().getDate()).isEqualTo(LocalDate.of(2025, 2, 10))
        );
        assertThat(saved).anySatisfy(r ->
                assertThat(r.getEvent().getDate()).isEqualTo(LocalDate.of(2025, 3, 5))
        );
        assertThat(saved).anySatisfy(r ->
                assertThat(r.getSendDatetime()).isEqualTo(expected1)
        );
        assertThat(saved).anySatisfy(r ->
                assertThat(r.getSendDatetime()).isEqualTo(expected2)
        );

        // Doğru şablon isimleri çağrılmış mı?
        verify(emailTemplateRepository).findByName("Freeze Template");
        verify(emailTemplateRepository).findByName("Release Template");
    }

    @Test
    void shouldDoNothing_whenUuidIsInvalid() throws Exception {
        // Act
        service.createRemindersForImportBatch("not-a-uuid");

        // Assert: hiçbir şey çağrılmamalı
        verify(importBatchEventRepository, never()).findByBatch_Id(any());
        verify(emailReminderRepository, never()).save(any());
    }

    @Test
    void shouldNotSave_whenTemplateMissing() throws Exception {
        // Arrange: FREEZE event var ama şablon bulunamayacak
        UUID batchId = UUID.randomUUID();
        when(importBatchEventRepository.findByBatch_Id(batchId)).thenReturn(List.of(
                ImportBatchEvent.builder().event(event(EventType.FREEZE, LocalDate.of(2025, 2, 10))).build()
        ));
        when(emailTemplateRepository.findByName("Freeze Template")).thenReturn(Optional.empty());


        // Act
        service.createRemindersForImportBatch(batchId.toString());

        // Assert: createReminder içinde exception yakalanıp loglanır → save hiç çağrılmaz
        verify(emailReminderRepository, never()).save(any());
        // ekstra güvence: grup hiç aranmadı
        verify(recipientGroupRepository, never()).findByGroupName(anyString());
    }


    @Test
    void shouldNotSave_whenRecipientGroupMissing() throws Exception {
        // Arrange: RELEASE event var, şablon var, grup yok
        UUID batchId = UUID.randomUUID();
        when(importBatchEventRepository.findByBatch_Id(batchId)).thenReturn(List.of(
                ImportBatchEvent.builder().event(event(EventType.RELEASE, LocalDate.of(2025, 3, 5))).build()
        ));
        when(emailTemplateRepository.findByName("Release Template"))
                .thenReturn(Optional.of(EmailTemplate.builder().id(2L).name("Release Template").build()));
        when(recipientGroupRepository.findByGroupName("All Stakeholders")).thenReturn(Optional.empty());

        // Act
        service.createRemindersForImportBatch(batchId.toString());

        // Assert
        verify(emailReminderRepository, never()).save(any());
    }

    // --- helper methods ---
    private Event event(EventType type, LocalDate date) {
        Color color = Color.builder().eventType(type).build();
        return Event.builder()
                .id(new Random().nextLong())
                .date(date)
                .color(color)
                .build();
    }

    @Test
    void shouldReturnImmediately_whenBatchHasNoEvents() throws Exception {
        // Arrange
        UUID batchId = UUID.randomUUID();
        when(importBatchEventRepository.findByBatch_Id(batchId)).thenReturn(Collections.emptyList());

        // Act
        service.createRemindersForImportBatch(batchId.toString());

        // Assert: hiçbir şey yapılmamalı
        verify(emailTemplateRepository, never()).findByName(anyString());
        verify(recipientGroupRepository, never()).findByGroupName(anyString());
        verify(emailReminderRepository, never()).save(any());
    }

    @Test
    void shouldSkip_whenBatchEventHasNullEvent() throws Exception {
        // Arrange
        UUID batchId = UUID.randomUUID();
        when(importBatchEventRepository.findByBatch_Id(batchId)).thenReturn(List.of(
                ImportBatchEvent.builder().event(null).build()
        ));

        // Act
        service.createRemindersForImportBatch(batchId.toString());

        // Assert: createReminder çağrılmamalı
        verify(emailTemplateRepository, never()).findByName(anyString());
        verify(recipientGroupRepository, never()).findByGroupName(anyString());
        verify(emailReminderRepository, never()).save(any());
    }

    @Test
    void shouldCreateReminders_forBusyDay_Patch_Drc_Templates() throws Exception {
        // Arrange: kalan 3 tip (switch dalları)
        UUID batchId = UUID.randomUUID();

        Event eBusy  = event(EventType.BUSY_DAY,        LocalDate.of(2025, 4, 10));
        Event ePatch = event(EventType.IMPORTANT_PATCH, LocalDate.of(2025, 5, 20));
        Event eDrc   = event(EventType.IMPORTANT_DRC,   LocalDate.of(2025, 6, 15));

        when(importBatchEventRepository.findByBatch_Id(batchId)).thenReturn(List.of(
                ImportBatchEvent.builder().event(eBusy).build(),
                ImportBatchEvent.builder().event(ePatch).build(),
                ImportBatchEvent.builder().event(eDrc).build()
        ));

        // Şablonlar (tam olarak bu isimlerle aranıyor)
        when(emailTemplateRepository.findByName("Busy Day Template"))
                .thenReturn(Optional.of(EmailTemplate.builder().id(10L).name("Busy Day Template").build()));
        when(emailTemplateRepository.findByName("Important Patch Template"))
                .thenReturn(Optional.of(EmailTemplate.builder().id(11L).name("Important Patch Template").build()));
        when(emailTemplateRepository.findByName("Important DRC Template"))
                .thenReturn(Optional.of(EmailTemplate.builder().id(12L).name("Important DRC Template").build()));

        // Grup
        when(recipientGroupRepository.findByGroupName("All Stakeholders"))
                .thenReturn(Optional.of(RecipientGroup.builder().id(1L).groupName("All Stakeholders").build()));

        // Kaydetme
        when(emailReminderRepository.save(any(EmailReminder.class))).thenAnswer(inv -> {
            EmailReminder r = inv.getArgument(0);
            r.setId(new Random().nextLong());
            return r;
        });

        // Act
        service.createRemindersForImportBatch(batchId.toString());

        // Assert
        verify(emailTemplateRepository, times(1)).findByName("Busy Day Template");
        verify(emailTemplateRepository, times(1)).findByName("Important Patch Template");
        verify(emailTemplateRepository, times(1)).findByName("Important DRC Template");
        verify(emailReminderRepository, times(3)).save(any(EmailReminder.class));
    }









}