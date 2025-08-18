package com.example.ReleaseRadar.controllers;

import static org.junit.jupiter.api.Assertions.*;

import com.example.ReleaseRadar.dtos.Response;
import com.example.ReleaseRadar.exceptions.ExcelProcessingException;
import com.example.ReleaseRadar.exceptions.FileValidationException;
import com.example.ReleaseRadar.exceptions.GlobalExceptionHandler;
import com.example.ReleaseRadar.services.importflow.ImportExcelServiceImpl;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = ImportExcelController.class)
@AutoConfigureMockMvc(addFilters = false) // Security filtreleri kapansın
@Import(GlobalExceptionHandler.class)     // Exception handler devreye girsin
class ImportExcelControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockitoBean
    private ImportExcelServiceImpl importExcelService;

    @Test
    void upload_ok_returns200_andBody() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "calendar.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes()
        );

        when(importExcelService.importExcel(any()))
                .thenReturn(Response.builder()
                        .status(200)
                        .message("Excel dosyası başarıyla yüklendi")
                        .build());

        mvc.perform(multipart("/api/import/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value(200))
                .andExpect(jsonPath("$.message").value("Excel dosyası başarıyla yüklendi"));
    }

    @Test
    void upload_emptyFile_returns400() throws Exception {
        MockMultipartFile empty = new MockMultipartFile(
                "file", "x.xlsx", "application/vnd.ms-excel", new byte[0]
        );

        when(importExcelService.importExcel(any()))
                .thenThrow(new FileValidationException("Excel dosyası boş."));

        mvc.perform(multipart("/api/import/upload")
                        .file(empty)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Excel dosyası boş."));
    }

    @Test
    void upload_serviceThrowsExcelProcessing_returns500() throws Exception {
        MockMultipartFile file = new MockMultipartFile(
                "file", "calendar.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "dummy".getBytes()
        );

        when(importExcelService.importExcel(any()))
                .thenThrow(new ExcelProcessingException("parse error"));

        mvc.perform(multipart("/api/import/upload")
                        .file(file)
                        .contentType(MediaType.MULTIPART_FORM_DATA))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Excel işleme sırasında bir hata oluştu."));
    }
}