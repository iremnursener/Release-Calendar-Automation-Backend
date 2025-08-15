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

/**
 * Excel dosyalarındaki takvim hücrelerini okuyup renge göre {@link Event} üreten servis.
 *
 * <p><b>Özet akış:</b> Dosyanın ilk sayfasını tarar → tarih hücrelerini tespit eder →
 * hücre dolgu rengini ARGB HEX formatında çıkarır → renge karşılık gelen {@link Color} kaydını
 * veritabanından eşler → {@link Event} oluşturur ve kaydeder → oluşturulan event’i
 * ilgili {@link ImportBatch} ile {@link ImportBatchEvent} üzerinden ilişkilendirir.</p>
 *
 * <p>Not: Renk eşlemesi için veritabanındaki renk kodları ARGB HEX (örn. {@code FFCC0000}) formatına normalize edilir.</p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class ExcelParserServiceImpl implements IExcelParserService {

    private final IImportBatchRepository importBatchRepository;
    private final IColorRepository colorRepository;
    private final IEventRepository eventRepository;
    private final IImportBatchEventRepository importBatchEventRepository;

    /**
     * Verilen {@code batchId} ile ilişkili içe aktarma bağlamında Excel dosyasını parse eder;
     * takvimdeki tarih+renkli hücrelerden {@link Event} kayıtları üretir ve batch ile ilişkilendirir.
     *
     * @param batchId İşlenecek içe aktarma partisinin benzersiz kimliği
     * @param file    Excel dosyası ({@code .xlsx} vb.)
     * @return Oluşturulan tüm {@link Event} kayıtlarının listesi
     * @throws IOException Dosya okuma/parsing sırasında IO hatası meydana gelirse
     * @throws RuntimeException {@link ImportBatch} bulunamazsa
     */
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

    /**
     * Tek bir hücreyi işleyip gerekirse {@link Event} ve {@link ImportBatchEvent} kayıtlarını oluşturur.
     * <ul>
     *   <li>Tarih formatlı ve numerik hücreler dikkate alınır</li>
     *   <li>Hücrenin dolgu rengi ARGB HEX formatında okunur ve renge karşılık gelen {@link Color} ile eşlenir</li>
     * </ul>
     *
     * @param cell         İşlenecek hücre
     * @param colorLegend  ARGB HEX → {@link Color} eşleme tablosu
     * @param batch        İlişkilendirilecek {@link ImportBatch}
     * @param parsedEvents Oluşturulan event’lerin ekleneceği liste
     */
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

    /**
     * Hücrenin efektif tipini döndürür.
     * <p>Hücre tipi FORMULA ise cached result tipi kullanılır.</p>
     *
     * @param cell Hücre
     * @return Efektif hücre tipi
     */
    private CellType getEffectiveCellType(Cell cell) {
        CellType type = cell.getCellType();
        if (type == CellType.FORMULA) {
            type = cell.getCachedFormulaResultType();
        }
        return type;
    }

    /**
     * Hücrenin tarih formatlı numerik bir hücre olup olmadığını kontrol eder.
     *
     * @param cell          Hücre
     * @param effectiveType Önceden çözümlenmiş hücre tipi
     * @return Tarih formatlı numerik hücre ise {@code true}, aksi halde {@code false}
     */
    private boolean isDateNumericCell(Cell cell, CellType effectiveType) {
        return effectiveType == CellType.NUMERIC && DateUtil.isCellDateFormatted(cell);
    }

    /**
     * Excel tarih hücresini sistem saat dilimine göre {@link LocalDate}’a dönüştürür.
     *
     * @param cell Tarih hücresi
     * @return Hücrenin tarih değeri (LocalDate)
     */
    private LocalDate toLocalDate(Cell cell) {
        Date date = cell.getDateCellValue();
        return date.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
    }


    /**
     * Hücrenin dolgu rengini ARGB HEX formatında döndürür (yalnızca XSSF için).
     *
     * @param cell Hücre
     * @return ARGB HEX (örn. {@code FFCC0000}) veya yoksa {@code null}
     */
    private String getCellFillArgbHex(Cell cell) {
        CellStyle style = cell.getCellStyle();
        if (style == null) return null;
        if (style.getFillForegroundColorColor() instanceof XSSFColor xssfColor) {
            return xssfColor.getARGBHex(); // orijinal koddaki gibi dönüş
        }
        return null;
    }

    /**
     * Verilen renk ve tarih bilgisiyle {@link Event} oluşturur, kaydeder ve
     * {@link ImportBatchEvent} ile batch’e bağlar.
     *
     * @param batch        İlgili {@link ImportBatch}
     * @param matchedColor Eşleşen {@link Color}
     * @param localDate    Etkinlik tarihi
     * @param parsedEvents Oluşturulan event’lerin ekleneceği liste
     */
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
    /**
     * Verilen parametrelerle yeni bir {@link Event} nesnesi oluşturur.
     *
     * @param batch İçe aktarma partisi (oluşturan kullanıcı bilgisi için)
     * @param color Renk (event tipini içerir)
     * @param date  Tarih
     * @return Kurulmuş {@link Event} (henüz kaydedilmemiş olabilir)
     */
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
    /**
     * Event’i batch ile ilişkilendiren {@link ImportBatchEvent} nesnesi oluşturur.
     *
     * @param batch İçe aktarma partisi
     * @param event Oluşturulan etkinlik
     * @return Kurulmuş {@link ImportBatchEvent}
     */
    private ImportBatchEvent buildImportBatchEvent(ImportBatch batch, Event event) {
        return ImportBatchEvent.builder()
                .batch(batch)
                .event(event)
                .tryCount(0) // silinebilir
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .build();
    }

    /**
     * Veritabanındaki tüm renk tanımlarını okuyup ARGB HEX anahtarına göre bir eşleme tablosu döndürür.
     *
     * @return {@code ARGB HEX → Color} eşleme tablosu
     */
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
    /**
     * Verilen HEX değeri normalleştirir.
     * <ul>
     *   <li>{@code #RRGGBB} → {@code FFRRGGBB} (ARGB’ye çevrilir)</li>
     *   <li>Harflendirmeler büyük harfe dönüştürülür</li>
     * </ul>
     *
     * @param hex Renk HEX değeri
     * @return Normalize edilmiş ARGB HEX
     */
    private String normalizeHex(String hex) {
        if (hex.startsWith("#")) {
            String rgb = hex.substring(1).toUpperCase();
            return "FF" + rgb;
        }
        return hex.toUpperCase();
    }
}



