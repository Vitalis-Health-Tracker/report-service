package com.example.report_service.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserDto {
    private Integer age;
    private float height;
    private float weight;
    private String gender;
    private double bmi;
    private String journey;
}
