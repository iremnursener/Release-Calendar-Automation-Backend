package com.example.ReleaseRadar.dtos;


import com.example.ReleaseRadar.entities.RecipientGroup;
import com.example.ReleaseRadar.enums.ReminderStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@AllArgsConstructor  //SİLİNEBİLİR
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) //null olan alanları JSON çıktısına dahil etme.
@JsonIgnoreProperties(ignoreUnknown = true)
// Gelen JSON'da tanımadığın ekstra alanlar varsa, hata fırlatmak yerine onları yok sayar.
@Data
public class EmailReminderDto {
    private Long id;
    private EventDto event;
    private Long template;
    private RecipientGroup group;
    private LocalDateTime sendDatetime;
    private ReminderStatus status;
    private LocalDateTime lastTryAt;
    private Boolean isActive;
    private LocalDateTime createdAt;
}
