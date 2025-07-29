package com.example.ReleaseRadar.dtos;

import com.example.ReleaseRadar.entities.Color;
import com.example.ReleaseRadar.entities.User;
import com.example.ReleaseRadar.enums.EventType;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) //null olan alanları JSON çıktısına dahil etme.
@JsonIgnoreProperties(ignoreUnknown = true)
// Gelen JSON'da tanımadığın ekstra alanlar varsa, hata fırlatmak yerine onları yok sayar.

public class EventDto {
    private Long id;
    private EventType eventType;
    private String title;
    private LocalDateTime startDate;
    private LocalDateTime endDate;
    private String description;
    private UserDto createdBy;
    private ColorDto color;
}
