package com.example.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class WellbeingDto {
    private Integer sleepTime;
    private String mood;
    private LocalDate wellbeingDate;
}
