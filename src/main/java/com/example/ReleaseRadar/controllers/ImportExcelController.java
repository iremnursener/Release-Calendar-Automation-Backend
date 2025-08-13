package com.example.ReleaseRadar.controllers;


import com.example.ReleaseRadar.dtos.Response;
import com.example.ReleaseRadar.services.importflow.ImportExcelServiceImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/import")  //değişebilir
@RequiredArgsConstructor
public class ImportExcelController {

    private final ImportExcelServiceImpl importExcelService;

    @PostMapping("/upload")
    public ResponseEntity<Response> uploadExcel(@Valid @RequestParam("file") MultipartFile file) {

        return ResponseEntity.ok(importExcelService.importExcel(file));


    }
}