package com.example.ReleaseRadar.services.importflow;

import com.example.ReleaseRadar.dtos.Response;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.User;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ImportExcelServiceImpl {

    private final IImportBatchRepository importBatchRepository;
    private final IUserRepository userRepository;
    private final ExcelParserServiceImpl excelParserService;
    private final EmailReminderServiceImpl emailReminderService;
    private final IImportChainFactory importChainFactory; // <-- eklendi

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
            throw new RuntimeException("Excel dosyası işlenirken hata oluştu: " + e.getMessage());
        }

        return Response.builder()
                .status(200)
                .message("Excel dosyası başarıyla yüklendi")
                .build();
    }

    // ------------------ SRP: Yardımcı metodlar ------------------

    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) { //size kontrolü ekle backend ve frontda
            throw new IllegalArgumentException("Excel dosyası boş.");
        }
    }

    private User loadUploaderUser() {
        return userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));
        //yeni exception tanımlanacak runtime olmasın
    }

    private Path persistFileToDesktop(MultipartFile file) {
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        log.info("Masaüstü: {}", desktopDir.getAbsolutePath());

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path fullPath = Path.of(desktopDir.getAbsolutePath(), fileName);

        try {
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Dosya yazma hatası: " + e.getMessage(), e);
        }

        return fullPath;
    }

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
