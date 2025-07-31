package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.Event;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import com.example.ReleaseRadar.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ImportExcelService {

    private final IImportBatchRepository importBatchRepository;
    private final IUserRepository userRepository;
    private final ExcelParserService excelParserService;


    public void importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel dosyası boş.");
        }


        // şu an login olan kullanıcı alınması gerekiyordu ama sabit şekilde id si 1 olan kullanıcı yüklemiş gibi yaptık,kullanıcı dbde önceden kayıtlı
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));


        //  ImportBatch oluştur
        ImportBatch batch = ImportBatch.builder()
                .fileName(file.getOriginalFilename())
                .notes("Excel ile yüklendi") // kullanıcıdan girdi alınarak yapılabilir
                .isActive(true)
                .uploadedBy(user)  // idsi 1 olan kullanıcı farz edildi
                .build();

        // 2. DB'ye kaydet --BURASI ÇALIŞIYOR
        ImportBatch savedBatch = importBatchRepository.save(batch); //bunu sonra dönebiliriz belki


        // 3. Excel parser ile dosyayı oku ve Event listesi al
        try {
            List<Event> events = excelParserService.parseExcelByBatch(savedBatch.getId(), file);
            // Eğer dönen listeyi kullanmak istersen burada kullanabilirsin.
            System.out.println("Toplam event sayısı: " + events.size()); //365 DÖNMESİ LAZIM YAKLAŞIK,RENKSİZLERİ DÖNMÜYOR OLABİLİR
        } catch (IOException e) {
            throw new RuntimeException("Excel dosyası işlenirken hata oluştu: " + e.getMessage());
        }

    }









}