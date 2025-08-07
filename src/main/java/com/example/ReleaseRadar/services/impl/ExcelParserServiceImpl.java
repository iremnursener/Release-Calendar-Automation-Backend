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
public class ExcelParserServiceImpl  implements IExcelParserService {

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
        Map<String, Color> colorLegend = loadColorLegendFromDB();


        for (int i = 0; i <= sheet.getLastRowNum(); i++) {
            Row row = sheet.getRow(i);
            if (row == null) continue;

            for (int j = 0; j < row.getLastCellNum(); j++) {
                Cell cell = row.getCell(j);
                String position = "(" + i + "," + j + ")";

                if (cell == null) {
                    continue;
                }

                CellType type = cell.getCellType();

                if (type == CellType.FORMULA) {
                    type = cell.getCachedFormulaResultType(); // Formül hücresi için gerçek tipi al
                }

                if (type == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell)) {
                    Date date = cell.getDateCellValue();
                    LocalDate localDate = date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();

                    // Hücrenin arka plan rengini al
                    CellStyle style = cell.getCellStyle();
                    String hexColor = null;
                    if (style != null && style.getFillForegroundColorColor() instanceof XSSFColor xssfColor) {
                        hexColor = xssfColor.getARGBHex();
                    }

                    // Renk eşleşmesini bul
                    Color matchedColor = null;
                    if (hexColor != null) {
                        matchedColor = colorLegend.get(hexColor);
                    }


                    //DB YE KAYDET
                    if (matchedColor != null ){
                        Event event = Event.builder()
                                .title(matchedColor.getEventType().toString())
                                .color(matchedColor)
                                .date(localDate)
                                .description("  ")  // event type a göre açıklama gelecek buraya
                                .createdBy(batch.getUploadedBy())
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        event = eventRepository.save(event); //dbye atıldı
                        parsedEvents.add(event);

                        ImportBatchEvent ibe = ImportBatchEvent.builder()
                                .batch(batch)
                                .event(event)
                                .tryCount(0)   //silinebilir
                                .isActive(true)
                                .createdAt(LocalDateTime.now())
                                .build();
                        importBatchEventRepository.save(ibe);
                    }
                }
            }
        }

        workbook.close();
        return parsedEvents;
    }


    public Map<String, Color> loadColorLegendFromDB() {
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

    public String normalizeHex(String hex) {
        if (hex.startsWith("#")) {
            String rgb = hex.substring(1).toUpperCase();
            return "FF" + rgb;
        }
        return hex.toUpperCase();
    }
}



