package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.Color;
import com.example.ReleaseRadar.entities.Event;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.ImportBatchEvent;
import com.example.ReleaseRadar.repositories.IColorRepository;
import com.example.ReleaseRadar.repositories.IEventRepository;
import com.example.ReleaseRadar.repositories.IImportBatchEventRepository;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFCellStyle;
import org.apache.poi.xssf.usermodel.XSSFColor;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelParserService {

    private final IImportBatchRepository importBatchRepository;
    private final IColorRepository colorRepository;
    private final IEventRepository eventRepository;
    private final IImportBatchEventRepository importBatchEventRepository;

    public void parseExcelByBatch(UUID batchId, MultipartFile file) throws IOException {
        ImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Import batch not found"));

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        Map<String, Color> colorLegend = loadColorLegendFromDB();

        Map<String, Integer> monthRowMap = new HashMap<>();
        monthRowMap.put("Ocak", 4);
        monthRowMap.put("Şubat", 4);
        monthRowMap.put("Mart", 4);
        monthRowMap.put("Nisan", 4);
        monthRowMap.put("Mayıs", 12);
        monthRowMap.put("Haziran", 12);
        monthRowMap.put("Temmuz", 12);
        monthRowMap.put("Ağustos", 12);
        monthRowMap.put("Eylül", 20);
        monthRowMap.put("Ekim", 20);
        monthRowMap.put("Kasım", 20);
        monthRowMap.put("Aralık", 20);

        Map<String, Integer> monthColMap = new HashMap<>();
        monthColMap.put("Ocak", 0);
        monthColMap.put("Şubat", 8);
        monthColMap.put("Mart", 16);
        monthColMap.put("Nisan", 24);
        monthColMap.put("Mayıs", 0);
        monthColMap.put("Haziran", 8);
        monthColMap.put("Temmuz", 16);
        monthColMap.put("Ağustos", 24);
        monthColMap.put("Eylül", 0);
        monthColMap.put("Ekim", 8);
        monthColMap.put("Kasım", 16);
        monthColMap.put("Aralık", 24);


        List<String> months = List.of("Ocak", "Şubat", "Mart", "Nisan", "Mayıs", "Haziran",
                "Temmuz", "Ağustos", "Eylül", "Ekim", "Kasım", "Aralık");

        for (String month : months) {
            int startRow = monthRowMap.get(month);
            int startCol = monthColMap.get(month);

            for (int rowOffset = 6; rowOffset < 12; rowOffset++) {
                Row row = sheet.getRow(startRow + rowOffset);
                if (row == null) continue;
                for (int colOffset = 0; colOffset < 7; colOffset++) {
                    Cell cell = row.getCell(startCol + colOffset);
                    if (cell == null || cell.getCellType() != CellType.NUMERIC) continue;

                    double value = cell.getNumericCellValue();
                    if (value <= 0 || value > 31) continue;

                    int day = (int) value;
                    int monthNumber = months.indexOf(month) + 1;
                    int year = 2025;
                    LocalDate date = LocalDate.of(year, monthNumber, day);

                    CellStyle style = cell.getCellStyle();
                    if (style != null && style.getFillForegroundColorColor() instanceof XSSFColor xssfColor) {
                        String hexColor = xssfColor.getARGBHex();
                        if (hexColor != null && colorLegend.containsKey(hexColor)) {
                            Color matchedColor = colorLegend.get(hexColor);

                            Event event = Event.builder()
                                    .title(matchedColor.getEventType().toString())
                                    .color(matchedColor)
                                    .date(date)
                                    .description("Excel'den yüklendi")
                                    .createdBy(batch.getUploadedBy())
                                    .isActive(true)
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            event = eventRepository.save(event);

                            ImportBatchEvent ibe = ImportBatchEvent.builder()
                                    .batch(batch)
                                    .event(event)
                                    .tryCount(0)
                                    .isActive(true)
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            importBatchEventRepository.save(ibe);
                        }
                    }
                }
            }
        }
        workbook.close();
    }

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



