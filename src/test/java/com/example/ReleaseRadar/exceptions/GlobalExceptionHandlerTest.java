package com.example.ReleaseRadar.exceptions;

import static org.junit.jupiter.api.Assertions.*;


import com.example.ReleaseRadar.controllers.DummyExceptionController;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(controllers = DummyExceptionController.class)
@AutoConfigureMockMvc(addFilters = false) // security filtreleri kapalı
@Import(GlobalExceptionHandler.class)     // advice'ı test context'e dahil et
class GlobalExceptionHandlerTest {

    @Autowired
    private MockMvc mvc;

    @Test
    void genericException_returns400_withMessage() throws Exception {
        mvc.perform(get("/ex/generic"))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentTypeCompatibleWith(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("boom"));
    }

    @Test
    void fileValidation_returns400() throws Exception {
        mvc.perform(get("/ex/file-validation"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Excel dosyası boş."));
    }

    @Test
    void userNotFound_returns404() throws Exception {
        mvc.perform(get("/ex/user-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Kullanıcı bulunamadı"));
    }

    @Test
    void fileStorage_returns500_withGenericMessage() throws Exception {
        mvc.perform(get("/ex/file-storage"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Dosya kaydetme sırasında bir hata oluştu."));
    }

    @Test
    void excelProcessing_returns500_withGenericMessage() throws Exception {
        mvc.perform(get("/ex/excel-processing"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("Excel işleme sırasında bir hata oluştu."));
    }

    @Test
    void templateNotFound_returns404() throws Exception {
        mvc.perform(get("/ex/template-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Şablon yok"));
    }

    @Test
    void recipientGroupNotFound_returns404() throws Exception {
        mvc.perform(get("/ex/recipient-group-not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Grup yok"));
    }

    @Test
    void invalidBatchId_returns400() throws Exception {
        mvc.perform(get("/ex/invalid-batch-id"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Batch id geçersiz"));
    }
}