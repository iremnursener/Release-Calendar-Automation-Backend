package com.example.ReleaseRadar.dtos;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class Response {

    //generic
    private int status;
    private String message;

    //Yüklenen dosya için
    private ImportBatchDto importBatchDto;


    //Email data output için eklenebilir

    private final LocalDateTime timestamp = LocalDateTime.now();

}