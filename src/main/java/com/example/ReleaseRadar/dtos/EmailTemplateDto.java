package com.example.ReleaseRadar.dtos;


import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) //null olan alanları JSON çıktısına dahil etme.
@JsonIgnoreProperties(ignoreUnknown = true)
// Gelen JSON'da tanımadığın ekstra alanlar varsa, hata fırlatmak yerine onları yok sayar.

public class EmailTemplateDto { // SİLİNEBİLİR
    private Long id;
    private String name;
    private String subjectTpl;
    private String bodyHtmlTpl;
    private String bodyTextTpl;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
