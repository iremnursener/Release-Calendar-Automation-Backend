package com.example.ReleaseRadar.dtos;


import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class EmailMessageDto implements Serializable {
    private Long emailReminderId;
    private String to;
    private String subject;
    private String body;

}
