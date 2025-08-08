package com.example.ReleaseRadar.services.impl;

import com.example.ReleaseRadar.entities.Color;
import com.example.ReleaseRadar.entities.Event;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.ImportBatchEvent;
import com.example.ReleaseRadar.repositories.IColorRepository;
import com.example.ReleaseRadar.repositories.IEventRepository;
import com.example.ReleaseRadar.repositories.IImportBatchEventRepository;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import com.example.ReleaseRadar.services.IExcelParserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelParserServiceImpl implements IExcelParserService {

    private final IImportBatchRepository importBatchRepository;
    private final IColorRepository colorRepository;
    private final IEventRepository eventRepository;
    private final IImportBatchEventRepository importBatchEventRepository;

    // ---- Bölünmüş sürüm ----
    public List<Event> parseExcelByBatch(UUID batchId, MultipartFile file) throws IOException {
        List<Event> parsedEvents = new ArrayList<>();

        ImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Import batch not found"));

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);
        Map<String, Color> colorLegend = loadColorLegendFromDB();

        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                if (cell == null) continue;

                processCell(cell, colorLegend, batch, parsedEvents);
            }
        }

        workbook.close();
        return parsedEvents;
    }

    // Tek bir hücreyi işler (tip tespiti, tarih kontrolü, renk çıkarma, DB kayıt)
    private void processCell(Cell cell,
                             Map<String, Color> colorLegend,
                             ImportBatch batch,
                             List<Event> parsedEvents) {

        CellType type = getEffectiveCellType(cell);
        if (!isDateNumericCell(cell, type)) return;

        LocalDate localDate = toLocalDate(cell);

        String hexColor = getCellFillArgbHex(cell); // örn: "FFCC0000"
        if (hexColor == null) return;

        Color matchedColor = colorLegend.get(hexColor);
        if (matchedColor == null) return;

        persistEventAndLink(batch, matchedColor, localDate, parsedEvents);
    }

    // FORMULA ise cached result tipini döndürür
    private CellType getEffectiveCellType(Cell cell) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return type;
    }

    // Hem numeric olmalı hem de Excel tarih formatı olmalı
    private boolean isDateNumericCell(Cell cell, CellType effectiveType) {
        return effectiveType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell);
    }

    private LocalDate toLocalDate(Cell cell) {
        Date date = cell.getDateCellValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }

    // Hücrenin dolgu rengini ARGB hex olarak döndürür (XSSF için)
    private String getCellFillArgbHex(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style == null) return null;
        if (style.getFillForegroundColorColor() instanceof XSSFColor xssfColor) {
            return xssfColor.getARGBHex(); // orijinal koddaki gibi dönüş
        }
        return null;
    }

    // Event ve ImportBatchEvent kayıtlarını yapar, listeye ekler
    private void persistEventAndLink(ImportBatch batch,
                                     Color matchedColor,
                                     LocalDate localDate,
                                     List<Event> parsedEvents) {

        Event event = buildEvent(batch, matchedColor, localDate);
        event = eventRepository.save(event);
        parsedEvents.add(event);

        ImportBatchEvent ibe = buildImportBatchEvent(batch, event);
        importBatchEventRepository.save(ibe);
    }

    private Event buildEvent(ImportBatch batch, Color color, LocalDate date) {
        return Event.builder()
                .title(color.getEventType().toString())
                .color(color)
                .date(date)
                .description("  ")  // event type'a göre açıklama gelecektir
                .createdBy(batch.getUploadedBy())
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    private ImportBatchEvent buildImportBatchEvent(ImportBatch batch, Event event) {
        return ImportBatchEvent.builder()
                .batch(batch)
                .event(event)
                .tryCount(0) // silinebilir
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    // --- Bunlara dokunmadım ---
    private Map<String, Color> loadColorLegendFromDB() {
        List<Color> colors = colorRepository.findAll();
        Map<String, Color> map = new HashMap<>();
        for (Color color : colors) {
            if (color.getColorHex() != null) {
                String hex = normalizeHex(color.getColorHex());
                map.put(hex, color);
            }
        }
        return map;
    }

    private String normalizeHex(String hex) {
        if (hex.startsWith("#")) {
            String rgb = hex.substring(1).toUpperCase();
            return "FF" + rgb;
        }
        return hex.toUpperCase();
    }
}



