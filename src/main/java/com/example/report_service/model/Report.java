package com.example.report_service.model;

import com.example.report_service.dto.DietDto;
import com.example.report_service.dto.FitnessDto;
import com.example.report_service.dto.UserDto;
import com.example.report_service.dto.WellbeingDto;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Document(collection = "report")
public class Report {
    @Id
    private String reportId;
    private String userId;
    private LocalDateTime reportDate;
    private List<DietDto> diet;
    private List<FitnessDto> fitness;
    private List<WellbeingDto> wellbeing;
    private UserDto user;
    private Float finalCaloriesConsumed;
    private Float finalCaloriesBurned;
    private String status;
    private Float userCalorie;
}
