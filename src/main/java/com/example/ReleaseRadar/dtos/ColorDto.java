package com.example.ReleaseRadar.dtos;


import com.example.ReleaseRadar.enums.CriticalityLevel;
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
public class ColorDto {
    private Long id;
    private String colorHex;
    private String colorName;
    private CriticalityLevel criticalityLevel;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
