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
import com.example.ReleaseRadar.services.importflow.handler.CreateRemindersHandler;
import com.example.ReleaseRadar.services.importflow.handler.ExcelParseHandler;
import com.example.ReleaseRadar.services.importflow.handler.ImportContext;
import com.example.ReleaseRadar.services.importflow.handler.ImportHandler;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.swing.filechooser.FileSystemView;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.UUID;


/**
 * Excel yükleme akışını yöneten servis.
 * <p>
 * Sorumluluklar:
 * <ul>
 *   <li>Yüklenen dosyanın doğrulanması</li>
 *   <li>Dosyanın disk (Masaüstü) üzerine kalıcı olarak kaydedilmesi</li>
 *   <li>İlgili {@link ImportBatch} kaydının oluşturulması</li>
 *   <li>İçe aktarma zincirinin (parse → hatırlatıcı oluşturma) tetiklenmesi</li>
 * </ul>
 * </p>
 */

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExcelServiceImpl {

    private final IImportBatchRepository importBatchRepository;
    private final IUserRepository userRepository;
    private final ExcelParserServiceImpl excelParserService;
    private final EmailReminderServiceImpl emailReminderService;
    private final IImportChainFactory importChainFactory;


    /**
     * Yüklenen Excel dosyasını doğrular, diske kaydeder, ilgili {@link ImportBatch}
     * kaydını oluşturur ve içe aktarma zincirini çalıştırır.
     *
     * <p><b>İşlem Adımları:</b> doğrulama → kalıcı kaydetme → batch kaydı → zinciri çalıştırma</p>
     *
     * @param file Yüklenen Excel dosyası ({@code MultipartFile})
     * @return Başarı/başarısızlık bilgisini içeren {@link Response}
     * @throws FileValidationException Dosya boş/geçersiz ise
     * @throws FileStorageException Dosya diske yazılırken hata oluşursa
     * @throws ExcelProcessingException Excel işleme/parsing sırasında hata oluşursa
     */
    @Transactional(rollbackOn = Exception.class)
    public Response importExcel(MultipartFile file) {

        validateFile(file);
        User user = loadUploaderUser();

        Path fullPath = persistFileToDesktop(file);             // dosyayı kaydet
        ImportBatch savedBatch = saveImportBatch(fullPath,user); // batch oluştur/kaydet


        ImportHandler chain = importChainFactory.create();
        // === Chain of Responsibility: Excel parse -> Email reminders ===
        try {
            chain.handle(new ImportContext(savedBatch.getId(), file));
        } catch (IOException e) {
            throw new ExcelProcessingException("Excel dosyası işlenirken hata oluştu: " + e.getMessage());
        }

        return Response.builder()
                .status(200)
                .message("Excel dosyası başarıyla yüklendi")
                .build();
    }



    // ------------------ Yardımcı metodlar ------------------

    /**
     * Yüklenen dosyanın temel geçerlilik kontrollerini yapar.
     * <ul>
     *     <li>Boş olup olmadığını kontrol eder</li>
     *     <li>(İsteğe bağlı) Maksimum boyut ve içerik-türü kontrolü eklenebilir</li>
     * </ul>
     *
     * @param file Doğrulanacak dosya
     * @throws FileValidationException Dosya {@code null} ya da boş ise
     */
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) { //size kontrolü ekle backend ve frontda
            throw new FileValidationException("Excel dosyası boş.");
        }
    }

    /**
     * Yükleyici kullanıcıyı (sistem kullanıcısı) veritabanından yükler.
     *
     * @return Bulunan {@link User}
     * @throws UserNotFoundException Kullanıcı bulunamazsa
     */
    private User loadUploaderUser() {
        return userRepository.findById(1L)
                .orElseThrow(() -> new UserNotFoundException("Kullanıcı bulunamadı"));
        //yeni exception tanımlanacak runtime olmasın
    }

    /**
     * Dosyayı kullanıcının Masaüstü dizinine kalıcı olarak kaydeder.
     * <p>Dosya adı çakışmalarını engellemek için rasgele bir UUID öneki kullanılır.</p>
     *
     * @param file Kaydedilecek dosya
     * @return Kaydedilen dosyanın tam yolu
     * @throws FileStorageException Yazma/oluşturma sırasında IO hatası olursa
     */
    private Path persistFileToDesktop(MultipartFile file) {
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        log.info("Masaüstü: {}", desktopDir.getAbsolutePath());

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path fullPath = Path.of(desktopDir.getAbsolutePath(), fileName);

        try {
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, file.getBytes());
        } catch (IOException e) {
            throw new FileStorageException("Dosya yazma hatası: " + e.getMessage() );
        }

        return fullPath;
    }

    /**
     * Verilen dosya yolu ve kullanıcı bilgisiyle yeni bir {@link ImportBatch} oluşturup kaydeder.
     *
     * @param fullPath Diske kaydedilmiş dosyanın tam yolu
     * @param user     Yüklemeyi yapan kullanıcı
     * @return Veritabanına kaydedilmiş {@link ImportBatch} örneği
     */
    private ImportBatch saveImportBatch(Path fullPath, User user) {
        ImportBatch batch = ImportBatch.builder()
                .filePath(fullPath.toString())
                .fileName(fullPath.getFileName().toString())
                .notes("Excel tablosu yüklendi") // constant tanımlayarak al
                .isActive(true)
                .uploadedBy(user)
                .build();

        return importBatchRepository.save(batch);
    }

}
