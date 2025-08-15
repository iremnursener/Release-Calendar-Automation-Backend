package com.example.ReleaseRadar.exceptions;

import com.example.ReleaseRadar.dtos.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice(basePackages = "com.example.ReleaseRadar.controllers")
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Response> handleException(Exception e) {
        return ResponseEntity.badRequest().body(
                Response.builder()
                        .status(400)
                        .message(e.getMessage())
                        .build()
        );
    }

    // 400 - Dosya geçersiz vb.
    @ExceptionHandler(FileValidationException.class)
    public ResponseEntity<Response> handleFileValidation(FileValidationException e) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Response.builder()
                        .status(HttpStatus.BAD_REQUEST.value())
                        .message(e.getMessage())
                        .build());
    }

    // 404 - Kullanıcı/varlık bulunamadı
    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<Response> handleUserNotFound(UserNotFoundException e) {
        return ResponseEntity.status(HttpStatus.NOT_FOUND)
                .body(Response.builder()
                        .status(HttpStatus.NOT_FOUND.value())
                        .message(e.getMessage())
                        .build());
    }

    // 500 - Dosya sistemi hataları
    @ExceptionHandler(FileStorageException.class)
    public ResponseEntity<Response> handleFileStorage(FileStorageException e) {
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Response.builder()
                        .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
                        .message("Dosya kaydetme sırasında bir hata oluştu.")
                        .build());
    }

    @ExceptionHandler(ExcelProcessingException.class)
    public ResponseEntity<Response> handleExcelProcessing(ExcelProcessingException e) {
        return ResponseEntity.status(500).body(
                Response.builder().status(500).message("Excel işleme sırasında bir hata oluştu.").build()
        );
    }

    @ExceptionHandler(TemplateNotFoundException.class)
    public ResponseEntity<Response> handleTemplateNotFound(TemplateNotFoundException e) {
        return ResponseEntity.status(404).body(
                Response.builder().status(404).message(e.getMessage()).build()
        );
    }

    @ExceptionHandler(RecipientGroupNotFoundException.class)
    public ResponseEntity<Response> handleRecipientGroupNotFound(RecipientGroupNotFoundException e) {
        return ResponseEntity.status(404).body(
                Response.builder().status(404).message(e.getMessage()).build()
        );
    }

    @ExceptionHandler(InvalidBatchIdException.class)
    public ResponseEntity<Response> handleInvalidBatchId(InvalidBatchIdException e) {
        return ResponseEntity.badRequest().body(
                Response.builder().status(400).message(e.getMessage()).build()
        );
    }





}
