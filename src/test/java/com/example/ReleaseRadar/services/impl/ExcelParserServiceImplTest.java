package com.example.ReleaseRadar.services.impl;



import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import com.example.ReleaseRadar.entities.Color;
import com.example.ReleaseRadar.entities.Event;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.ImportBatchEvent;
import com.example.ReleaseRadar.repositories.IColorRepository;
import com.example.ReleaseRadar.repositories.IEventRepository;
import com.example.ReleaseRadar.repositories.IImportBatchEventRepository;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import org.apache.poi.ss.usermodel.FillPatternType;
import org.apache.poi.xssf.usermodel.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.util.*;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;


@ExtendWith(MockitoExtension.class)
class ExcelParserServiceImplTest {

    @Mock private IImportBatchRepository importBatchRepository;
    @Mock private IColorRepository colorRepository;
    @Mock private IEventRepository eventRepository;
    @Mock private IImportBatchEventRepository importBatchEventRepository;

    @InjectMocks
    private ExcelParserServiceImpl service;

    private UUID batchId;
    private ImportBatch batch;

    @BeforeEach
    void setUp() {
        batchId = UUID.randomUUID();
        batch = ImportBatch.builder()
                .id(batchId)
                .uploadedBy(com.example.ReleaseRadar.entities.User.builder().id(1L).build())
                .build();


    }



    @Test
    void shouldCreateEvent_whenDateCellHasMatchingColor() throws Exception {

        when(importBatchRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(batch));
        // Arrange
        // DB'deki renk efsanesi: ARGB FFCC0000 -> FREEZE
        Color red = Color.builder()
                .id(1L)
                .colorHex("FFCC0000")
                .eventType(com.example.ReleaseRadar.enums.EventType.FREEZE)
                .build();
        when(colorRepository.findAll()).thenReturn(List.of(red));

        // event kaydederken id dönelim (servis listeye saved event'i ekliyor)
        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0);
            e.setId(100L);
            return e;
        });

        MockMultipartFile file = buildOneCellExcel(
                LocalDate.of(2025, 1, 15),
                new XSSFColor(new java.awt.Color(0xCC, 0x00, 0x00), null) // #CC0000 -> FFCC0000
        );

        // Act
        List<Event> result = service.parseExcelByBatch(batchId, file);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).getColor().getId()).isEqualTo(1L);
        verify(eventRepository, times(1)).save(any(Event.class));
        verify(importBatchEventRepository, times(1)).save(any(ImportBatchEvent.class));
    }

    @Test
    void shouldReturnEmpty_whenColorDoesNotMatch() throws Exception {

        when(importBatchRepository.findById(any(UUID.class)))
                .thenReturn(Optional.of(batch));
        // Arrange: DB'de hiç renk yok → eşleşme olmayacak
        when(colorRepository.findAll()).thenReturn(Collections.emptyList());

        MockMultipartFile file = buildOneCellExcel(
                LocalDate.of(2025, 1, 15),
                new XSSFColor(new java.awt.Color(0x00, 0xFF, 0x00), null) // yeşil, DB'de yok
        );

        // Act
        List<Event> result = service.parseExcelByBatch(batchId, file);

        // Assert
        assertThat(result).isEmpty();
        verify(eventRepository, never()).save(any());
        verify(importBatchEventRepository, never()).save(any());
    }


    @Test
    void shouldThrow_whenBatchNotFound() {
        // Arrange
        UUID unknown = UUID.randomUUID();
        when(importBatchRepository.findById(unknown)).thenReturn(Optional.empty());

        MockMultipartFile file = new MockMultipartFile("file", "empty.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", new byte[0]);

        // Act & Assert
        assertThatThrownBy(() -> service.parseExcelByBatch(unknown, file))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("Import batch not found");
    }

    // --- helper method: tek tarih+renk hücreli in-memory Excel üretir ---
    private MockMultipartFile buildOneCellExcel(LocalDate date, XSSFColor fillColor) throws Exception {
        XSSFWorkbook wb = new XSSFWorkbook();
        XSSFSheet sheet = wb.createSheet("Sheet1");
        XSSFRow row = sheet.createRow(0);
        XSSFCell cell = row.createCell(0);

        // tarih değeri
        cell.setCellValue(java.util.Date.from(
                date.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()
        ));

        // tarih formatı + dolgu rengi
        XSSFCellStyle style = wb.createCellStyle();
        style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("m/d/yy"));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setFillForegroundColor(fillColor);
        cell.setCellStyle(style);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        wb.write(out);
        wb.close();

        return new MockMultipartFile(
                "file",
                "test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );
    }

    @Test
    void shouldIgnore_whenDateCellHasNoStyle() throws Exception {
        stubBatchFound();
        when(colorRepository.findAll()).thenReturn(Collections.emptyList()); // önemli değil

        // Tarih değeri var ama HİÇ stil set etmiyoruz
        XSSFWorkbook wb = new XSSFWorkbook();
        var sheet = wb.createSheet();
        var row = sheet.createRow(0);
        var cell = row.createCell(0);
        LocalDate d = LocalDate.of(2025,1,10);
        cell.setCellValue(java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        // stil yok!

        var file = toMultipart(wb);

        var result = service.parseExcelByBatch(UUID.randomUUID(), file);

        assertThat(result).isEmpty(); // hexColor=null → event oluşmaz
        verify(eventRepository, never()).save(any());
    }

    @Test
    void shouldHandleFormulaCell_cachedTypeUsed() throws Exception {
        stubBatchFound();
        // Renk eşleşmesin, önemli değil; amaç FORMULA yolunu tetiklemek
        when(colorRepository.findAll()).thenReturn(Collections.emptyList());

        XSSFWorkbook wb = new XSSFWorkbook();
        var sh = wb.createSheet();
        var r = sh.createRow(0);
        // A1: tarih
        var a1 = r.createCell(0);
        LocalDate d = LocalDate.of(2025,2,2);
        a1.setCellValue(java.util.Date.from(d.atStartOfDay(java.time.ZoneId.systemDefault()).toInstant()));
        var style = wb.createCellStyle();
        style.setDataFormat(wb.getCreationHelper().createDataFormat().getFormat("m/d/yy"));
        a1.setCellStyle(style);

        // B1: FORMÜL → A1
        var b1 = r.createCell(1);
        b1.setCellFormula("A1"); // FORMULA dalı

        var file = toMultipart(wb);

        var result = service.parseExcelByBatch(UUID.randomUUID(), file);
        assertThat(result).isEmpty(); // renk yok → event yok; ama FORMULA dalı sayıldı
    }

    @Test
    void shouldNormalizeHashHex_inLegend() throws Exception {
        stubBatchFound();

        // DB: "#CC0000" → normalize ile "FFCC0000" olacak
        var color = com.example.ReleaseRadar.entities.Color.builder()
                .id(1L).colorHex("#CC0000")
                .eventType(com.example.ReleaseRadar.enums.EventType.FREEZE)
                .build();
        when(colorRepository.findAll()).thenReturn(List.of(color));

        when(eventRepository.save(any(Event.class))).thenAnswer(inv -> {
            Event e = inv.getArgument(0); e.setId(1L); return e;
        });

        // Hücre rengi #CC0000 (ARGB: FFCC0000)
        var file = buildOneCellExcel(
                LocalDate.of(2025,3,3),
                new org.apache.poi.xssf.usermodel.XSSFColor(new java.awt.Color(0xCC,0x00,0x00), null)
        );

        var result = service.parseExcelByBatch(UUID.randomUUID(), file);
        assertThat(result).hasSize(1); // normalizeHex'in '#' dalı da kapsandı
    }

    // --- helper methods

    private void stubBatchFound() {
        when(importBatchRepository.findById(any(UUID.class))).thenReturn(
                Optional.of(ImportBatch.builder()
                        .id(UUID.randomUUID())
                        .uploadedBy(com.example.ReleaseRadar.entities.User.builder().id(1L).build())
                        .build())
        );
    }
    private MockMultipartFile toMultipart(XSSFWorkbook wb) throws Exception {
        var out = new java.io.ByteArrayOutputStream(); wb.write(out); wb.close();
        return new org.springframework.mock.web.MockMultipartFile(
                "file","test.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                out.toByteArray()
        );
    }




}