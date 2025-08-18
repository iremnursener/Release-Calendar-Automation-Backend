package com.example.ReleaseRadar.controllers;

import com.example.ReleaseRadar.exceptions.*;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/ex")
public class DummyExceptionController {

    @GetMapping("/generic")
    public void generic() { throw new RuntimeException("boom"); }

    @GetMapping("/file-validation")
    public void fileValidation() { throw new FileValidationException("Excel dosyası boş."); }

    @GetMapping("/user-not-found")
    public void userNotFound() { throw new UserNotFoundException("Kullanıcı bulunamadı"); }

    @GetMapping("/file-storage")
    public void fileStorage() { throw new FileStorageException("disk error"); }

    @GetMapping("/excel-processing")
    public void excelProcessing() { throw new ExcelProcessingException("parse fail"); }

    @GetMapping("/template-not-found")
    public void templateNotFound() { throw new TemplateNotFoundException("Şablon yok"); }

    @GetMapping("/recipient-group-not-found")
    public void recipientGroupNotFound() { throw new RecipientGroupNotFoundException("Grup yok"); }

    @GetMapping("/invalid-batch-id")
    public void invalidBatchId() { throw new InvalidBatchIdException("Batch id geçersiz"); }
}
