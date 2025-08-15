package com.example.ReleaseRadar.services.importflow;

import com.example.ReleaseRadar.dtos.Response;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.exceptions.ExcelProcessingException;
import com.example.ReleaseRadar.exceptions.FileStorageException;
import com.example.ReleaseRadar.exceptions.FileValidationException;
import com.example.ReleaseRadar.exceptions.UserNotFoundException;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import com.example.ReleaseRadar.repositories.IUserRepository;
import com.example.ReleaseRadar.services.impl.EmailReminderServiceImpl;
import com.example.ReleaseRadar.services.impl.ExcelParserServiceImpl;
import com.example.ReleaseRadar.services.importflow.handler.ImportContext;
import com.example.ReleaseRadar.services.importflow.handler.ImportHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.*;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ImportExcelServiceImplTest {

    @Mock
    private IImportBatchRepository importBatchRepository;
    @Mock private IUserRepository userRepository;
    @Mock private ExcelParserServiceImpl excelParserService;
    @Mock private EmailReminderServiceImpl emailReminderService;
    @Mock private IImportChainFactory importChainFactory;
    @Mock private ImportHandler chain;


    private ImportExcelServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new ImportExcelServiceImpl(
                importBatchRepository,
                userRepository,
                excelParserService,
                emailReminderService,
                importChainFactory
        );
    }

    private MultipartFile sampleFile() {
        return new MockMultipartFile(
                "file",
                "calendar.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes()
        );
    }

    @Test
    void importExcel_success_returns200_and_runsChain() throws Exception {
        // arrange
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        // importBatchRepository.save(...) id set edecek
        UUID batchId = UUID.randomUUID();
        when(importBatchRepository.save(any(ImportBatch.class)))
                .thenAnswer(inv -> {
                    ImportBatch in = inv.getArgument(0);
                    return ImportBatch.builder()
                            .id(batchId)
                            .fileName(in.getFileName())
                            .filePath(in.getFilePath())
                            .uploadedBy(in.getUploadedBy())
                            .isActive(in.getIsActive())
                            .notes(in.getNotes())
                            .build();
                });

        when(importChainFactory.create()).thenReturn(chain);
        // chain.handle başarıyla çalışsın
        doNothing().when(chain).handle(any(ImportContext.class));

        // Files.createDirectories / Files.write çağrılarını no-op yapmak için static mock
        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.createDirectories(any())).thenAnswer(inv -> inv.getArgument(0));
            files.when(() -> Files.write(any(Path.class), any(byte[].class))).thenAnswer(inv -> inv.getArgument(0));

            // act
            Response res = service.importExcel(sampleFile());

            // assert
            assertEquals(200, res.getStatus());
            assertEquals("Excel dosyası başarıyla yüklendi", res.getMessage());

            // ImportBatch kaydı doğru kullanıcıyla oluşturuldu mu?
            ArgumentCaptor<ImportBatch> batchCap = ArgumentCaptor.forClass(ImportBatch.class);
            verify(importBatchRepository).save(batchCap.capture());
            ImportBatch saved = batchCap.getValue();
            assertNotNull(saved.getFileName());
            assertNotNull(saved.getFilePath());
            assertEquals(user.getId(), saved.getUploadedBy().getId());

            // Chain doğru parametreyle tetiklendi mi?
            ArgumentCaptor<ImportContext> ctxCap = ArgumentCaptor.forClass(ImportContext.class);
            verify(chain).handle(ctxCap.capture());
            ImportContext ctx = ctxCap.getValue();
            assertEquals(batchId, ctx.batchId());
            assertNotNull(ctx.file());
        }
    }

    @Test
    void importExcel_nullFile_throwsFileValidationException() {
        assertThrows(FileValidationException.class, () -> service.importExcel(null));
        verifyNoInteractions(importBatchRepository, importChainFactory);
    }

    @Test
    void importExcel_emptyFile_throwsFileValidationException() {
        MultipartFile empty = new MockMultipartFile("file", "x.xlsx", "application/vnd.ms-excel", new byte[0]);
        assertThrows(FileValidationException.class, () -> service.importExcel(empty));
        verifyNoInteractions(importBatchRepository, importChainFactory);
    }

    @Test
    void importExcel_userNotFound_throwsUserNotFoundException() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        // Files static çağrılarına hiç gelmeden patlamalı
        assertThrows(UserNotFoundException.class, () -> service.importExcel(sampleFile()));
        verifyNoInteractions(importBatchRepository, importChainFactory);
    }

    @Test
    void importExcel_diskWriteFails_throwsFileStorageException() {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));


        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.createDirectories(any())).thenAnswer(inv -> inv.getArgument(0));
            files.when(() -> Files.write(any(Path.class), any(byte[].class)))
                    .thenThrow(new IOException("disk full"));

            assertThrows(FileStorageException.class, () -> service.importExcel(sampleFile()));
            verifyNoInteractions(importChainFactory); // chain’e gelmeden patlar
        }
    }

    @Test
    void importExcel_chainThrowsIOException_wrapsAsExcelProcessingException() throws Exception {
        User user = User.builder().id(1L).build();
        when(userRepository.findById(1L)).thenReturn(Optional.of(user));

        UUID batchId = UUID.randomUUID();
        when(importBatchRepository.save(any(ImportBatch.class)))
                .thenReturn(ImportBatch.builder().id(batchId).build());

        when(importChainFactory.create()).thenReturn(chain);
        doThrow(new IOException("parse fail")).when(chain).handle(any(ImportContext.class));

        try (MockedStatic<Files> files = mockStatic(Files.class)) {
            files.when(() -> Files.createDirectories(any())).thenAnswer(inv -> inv.getArgument(0));
            files.when(() -> Files.write(any(Path.class), any(byte[].class))).thenAnswer(inv -> inv.getArgument(0));

            assertThrows(ExcelProcessingException.class, () -> service.importExcel(sampleFile()));
            verify(chain).handle(any(ImportContext.class));
        }
    }






}