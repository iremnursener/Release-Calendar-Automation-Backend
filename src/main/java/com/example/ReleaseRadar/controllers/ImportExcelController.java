package com.example.ReleaseRadar.controllers;

import com.example.ReleaseRadar.services.ImportExcelService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")
@RequiredArgsConstructor
public class ImportExcelController {

    private final ImportExcelService importExcelService;

    @PostMapping("/upload")
    public ResponseEntity<String> uploadExcel(@RequestParam("file") MultipartFile file) {
        try {
            importExcelService.importExcel(file);
            return ResponseEntity.ok("Excel dosyası başarıyla yüklendi.");
        } catch (Exception e) {
            return ResponseEntity.badRequest().body("Yükleme hatası: " + e.getMessage());
        }
    }
}