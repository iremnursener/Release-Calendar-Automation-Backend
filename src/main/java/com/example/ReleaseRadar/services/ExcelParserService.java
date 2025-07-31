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
import java.time.ZoneId;
import java.util.*;

@Service
@RequiredArgsConstructor
public class ExcelParserService {

    private final IImportBatchRepository importBatchRepository;
    private final IColorRepository colorRepository;
    private final IEventRepository eventRepository;
    private final IImportBatchEventRepository importBatchEventRepository;

    public List<Event> parseExcelByBatch(UUID batchId, MultipartFile file) throws IOException {

        List<Event> parsedEvents = new ArrayList<>();

        ImportBatch batch = importBatchRepository.findById(batchId)
                .orElseThrow(() -> new RuntimeException("Import batch not found"));

        Workbook workbook = WorkbookFactory.create(file.getInputStream());
        Sheet sheet = workbook.getSheetAt(0);

        //  Tüm hücreleri başta logla -analiz
        System.out.println("==== TÜM HÜCRELERİ TARA ====");
        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                String position = "(" + i + "," + j + ")";

                if (cell == null) {
                    System.out.println(" Hücre " + position + ": null");
                    continue;
                }

                CellType type = cell.getCellType();
                if (type == CellType.FORMULA) {
                    type = cell.getCachedFormulaResultType();
                }

                // Tarih mi kontrolü
                if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
                    System.out.println(" Hücre " + position + " Tip: DATE | Tarih: " + localDate);
                } else {
                    // Diğer tipler için normal içerik yazdır
                    System.out.println(" Hücre " + position + " Tip: " + type + " | İçerik: " + cell.toString());
                }
            }
        }

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

        System.out.println("==== TAKVİM VERİLERİNİ OKU ====");
        for (String month : months) {
            int startRow = monthRowMap.get(month);
            int startCol = monthColMap.get(month);

            for (int rowOffset = 6; rowOffset < 12; rowOffset++) {
                Row row = sheet.getRow(startRow + rowOffset);
                if (row == null) continue;

                for (int colOffset = 0; colOffset < 7; colOffset++) {
                    Cell cell = row.getCell(startCol + colOffset);
                    String logPrefix = "Hücre (" + (startRow + rowOffset) + "," + (startCol + colOffset) + "): ";

                    if (cell == null) {
                        System.out.println(logPrefix + "Hücre null.");
                        continue;
                    }

                    // Formül destekli hücre tipi kontrolü
                    CellType type = cell.getCellType();
                    if (type == CellType.FORMULA) {
                        type = cell.getCachedFormulaResultType();
                    }

                    if (type != CellType.NUMERIC) {
                        System.out.println(logPrefix + "Tip: " + type + ", İçerik: " + cell.toString());
                        continue;
                    }

                    double value = cell.getNumericCellValue();
                    if (value <= 0 || value > 31) {
                        System.out.println(logPrefix + "Geçersiz gün değeri: " + value);
                        continue;
                    }

                    int day = (int) value;
                    int monthNumber = months.indexOf(month) + 1;
                    int year = 2025;
                    LocalDate date = LocalDate.of(year, monthNumber, day);

                    System.out.println(logPrefix + "Tarih: " + date);

                    // Renk alma
                    CellStyle style = cell.getCellStyle();
                    if (style != null && style.getFillForegroundColorColor() instanceof XSSFColor xssfColor) {
                        String hexColor = xssfColor.getARGBHex();
                        System.out.println(logPrefix + "Hex Renk: " + hexColor);

                        if (hexColor != null && colorLegend.containsKey(hexColor)) {
                            Color matchedColor = colorLegend.get(hexColor);
                            System.out.println(logPrefix + "Eşleşen Renk: " + matchedColor.getColorName());

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
                            parsedEvents.add(event);

                            ImportBatchEvent ibe = ImportBatchEvent.builder()
                                    .batch(batch)
                                    .event(event)
                                    .tryCount(0)
                                    .isActive(true)
                                    .createdAt(LocalDateTime.now())
                                    .build();
                            importBatchEventRepository.save(ibe);
                        } else {
                            System.out.println(logPrefix + "Bu renk için eşleşen bir anlam bulunamadı.");
                        }
                    } else {
                        System.out.println(logPrefix + "Renk bilgisi yok veya okunamıyor.");
                    }
                }
            }
        }

        workbook.close();
        return parsedEvents;
    }



//    public List<Event> parseExcelByBatch(UUID batchId, MultipartFile file) throws IOException {
//        List<Event> events = new ArrayList<>();
//
//        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
//            Sheet sheet = workbook.getSheetAt(0);
//
//            Map<String, Integer> monthMap = new HashMap<>();
//            monthMap.put("Ocak", 1);
//            monthMap.put("Şubat", 2);
//            monthMap.put("Mart", 3);
//            monthMap.put("Nisan", 4);
//            monthMap.put("Mayıs", 5);
//            monthMap.put("Haziran", 6);
//            monthMap.put("Temmuz", 7);
//            monthMap.put("Ağustos", 8);
//            monthMap.put("Eylül", 9);
//            monthMap.put("Ekim", 10);
//            monthMap.put("Kasım", 11);
//            monthMap.put("Aralık", 12);
//
//            for (int rowIndex = 0; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
//                Row row = sheet.getRow(rowIndex);
//                if (row == null) continue;
//
//                for (int colIndex = 0; colIndex < row.getLastCellNum(); colIndex++) {
//                    Cell cell = row.getCell(colIndex);
//                    if (cell == null || cell.getCellType() != CellType.STRING) continue;
//
//                    String cellValue = cell.getStringCellValue().trim();
//                    if (monthMap.containsKey(cellValue)) {
//                        int month = monthMap.get(cellValue);
//                        int startRow = rowIndex;
//                        int startCol = colIndex;
//
//                        System.out.println("🔍 Ay bulundu: " + cellValue + " (" + month + ") | Konum: (" + startRow + "," + startCol + ")");
//
//                        for (int r = startRow + 3; r < startRow + 9; r++) {
//                            Row dayRow = sheet.getRow(r);
//                            if (dayRow == null) continue;
//
//                            for (int c = startCol; c < startCol + 7; c++) {
//                                Cell dayCell = dayRow.getCell(c);
//                                if (dayCell == null) {
//                                    System.out.println("⛔ Boş hücre: (" + r + "," + c + ")");
//                                    continue;
//                                }
//
//                                if (dayCell.getCellType() != CellType.NUMERIC) {
//                                    System.out.println("⛔ Hücre numerik değil: (" + r + "," + c + ") -> " + dayCell.toString());
//                                    continue;
//                                }
//
//                                double val = dayCell.getNumericCellValue();
//                                if (val <= 0 || val > 31) {
//                                    System.out.println("⛔ Geçersiz gün değeri: " + val + " at (" + r + "," + c + ")");
//                                    continue;
//                                }
//
//                                int day = (int) val;
//                                LocalDate date = LocalDate.of(2025, month, day);
//                                System.out.println("📅 Tarih bulundu: " + date + " at (" + r + "," + c + ")");
//
//                                CellStyle style = dayCell.getCellStyle();
//                                if (style != null && style.getFillForegroundColorColor() instanceof XSSFColor xssfColor) {
//                                    String hex = xssfColor.getARGBHex();
//                                    System.out.println("🎨 Renk bulundu: " + hex + " | Tarih: " + date);
//
//                                    if (hex != null && colorRepository.findByColorHexIgnoreCase(hex).isPresent()) {
//                                        Color colorEntity = colorRepository.findByColorHexIgnoreCase(hex).get();
//
//                                        Event event = Event.builder()
//                                                .date(date)
//                                                .color(colorEntity)
//                                                .title(colorEntity.getEventType().toString())
//                                                .description("Excel'den yüklendi")
//                                                .isActive(true)
//                                                .createdAt(LocalDateTime.now())
//                                                .createdBy(importBatchRepository.findById(batchId).orElseThrow().getUploadedBy())
//                                                .build();
//
//                                        eventRepository.save(event);
//                                        events.add(event);
//                                        System.out.println("✅ Event kaydedildi: " + event.getTitle() + " - " + date);
//                                    } else {
//                                        System.out.println("⚠️ Renk eşleşmedi: " + hex);
//                                    }
//                                } else {
//                                    System.out.println("⚠️ Hücrede renk yok: (" + r + "," + c + ")");
//                                }
//                            }
//                        }
//                    }
//                }
//            }
//        }
//
//        return events;
//    }   //  salla bunu


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



