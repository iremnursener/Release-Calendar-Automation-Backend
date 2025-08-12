package com.example.ReleaseRadar.exceptions;

import com.example.ReleaseRadar.dtos.Response;
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
}
