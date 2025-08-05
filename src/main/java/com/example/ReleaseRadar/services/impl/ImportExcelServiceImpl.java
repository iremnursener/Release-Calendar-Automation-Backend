package com.example.ReleaseRadar.services.impl;

import com.example.ReleaseRadar.dtos.Response;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import com.example.ReleaseRadar.repositories.IUserRepository;
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


    public Response importExcel(MultipartFile file) {


        if (file == null || file.isEmpty()) { //DOSYA DOLU MU KONTROL
            throw new IllegalArgumentException("Excel dosyası boş.");
        }

        User user = userRepository.findById(1L) //ID=1 OLAN KULLANICININ YÜKLEDİĞİ FARZ EDİLİR
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));


        //DOSYAYI DESKTOPA KAYDET
        File desktopDir = FileSystemView.getFileSystemView().getHomeDirectory();
        System.out.println("Masaüstü: " + desktopDir.getAbsolutePath());

        String fileName = UUID.randomUUID() + "_" + file.getOriginalFilename();
        Path fullPath = Path.of(desktopDir.getAbsolutePath(), fileName);

        String filePathForDb = fullPath.toString();
        log.info("Oluşturulan dosya yolu (DB için): " + filePathForDb);

        try {
            Files.createDirectories(fullPath.getParent());
            Files.write(fullPath, file.getBytes());
        } catch (IOException e) {
            throw new RuntimeException("Dosya yazma hatası: " + e.getMessage(), e);
        }

        //BURADAN SONRASI SERVİSLERE BÖLÜNEBİLİR

        //  ImportBatch oluştur
        ImportBatch batch = ImportBatch.builder()
                .filePath(filePathForDb)
                .fileName(fileName)
                .notes("Excel tablosu yüklendi") // kullanıcıdan girdi alınarak yapılabilir
                .isActive(true)
                .uploadedBy(user)
                .build();

        // 2. DB'ye kaydet
        ImportBatch savedBatch = importBatchRepository.save(batch); //bunu sonra dönebiliriz belki


        // EXCEL DOSYASINI PARSE EDEN SERVİSİ ÇAĞIR VE EMAİL REMİNDERLARI OLUŞTUR
        try {

            excelParserService.parseExcelByBatch(savedBatch.getId(), file);

            emailReminderService.createRemindersForImportBatch(savedBatch.getId().toString());

        } catch (IOException e) {
            throw new RuntimeException("Excel dosyası işlenirken hata oluştu: " + e.getMessage());
        }


        return Response.builder()
                .status(200)
                .message("Excel dosyası başarıyla yüklendi")
                .build();

    }


}
