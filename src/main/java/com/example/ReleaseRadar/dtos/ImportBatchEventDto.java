package com.example.ReleaseRadar.dtos;


import com.example.ReleaseRadar.entities.Event;
import com.example.ReleaseRadar.entities.ImportBatch;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL) //null olan alanları JSON çıktısına dahil etme.
@JsonIgnoreProperties(ignoreUnknown = true)
// Gelen JSON'da tanımadığın ekstra alanlar varsa, hata fırlatmak yerine onları yok sayar.

public class ImportBatchEventDto {
    private Long id;
    private ImportBatchDto batch;
    private EventDto event;
    private Integer tryCount;
}
