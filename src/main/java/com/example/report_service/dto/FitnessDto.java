package com.example.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class FitnessDto {
    private LocalDateTime fitnessDate;
    public Float totalCaloriesBurned;
}
