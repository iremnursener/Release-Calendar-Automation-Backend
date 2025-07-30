package com.example.ReleaseRadar.services;

import com.example.ReleaseRadar.entities.ImportBatch;
import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.repositories.IImportBatchRepository;
import com.example.ReleaseRadar.repositories.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class ImportExcelService {

    private final IImportBatchRepository importBatchRepository;
    private final IUserRepository userRepository;





    public void importExcel(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("Excel dosyası boş.");
        }


        // Örn: şu an login olan kullanıcı
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new RuntimeException("Kullanıcı bulunamadı"));


        // 1. ImportBatch oluştur
        ImportBatch batch = ImportBatch.builder()
                .fileName(file.getOriginalFilename())
                .notes("Excel ile yüklendi") // kullanıcıdan girdi alınarak yapılabilir
                .isActive(true)
                .uploadedBy(user)
                .build();

        // 2. DB'ye kaydet
        ImportBatch savedBatch = importBatchRepository.save(batch); //bunu sonra dönebiliriz belki

    }




        // 3. Excel parser ile dosyayı oku ve Event listesi al
       // List<Event> events = excelParserService.parse(file);



        // 4. Her bir event için ImportBatchEvent oluştur ve kaydet
       // List<ImportBatchEvent> batchEvents = events.stream()
             //   .map(event -> {
                 //   ImportBatchEvent ibe = new ImportBatchEvent();
                   // ibe.setBatch(savedBatch);
                   // ibe.setEvent(event);
                   // ibe.setIsActive(true);
                   // ibe.setTryCount(0);
                   // ibe.setCreatedAt(LocalDateTime.now());
                 //   return ibe;
              //  }).toList();

        //importBatchEventRepository.saveAll(batchEvents);

}