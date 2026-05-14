package com.example.dto.assessment;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class AssessmentAdviceRow {
    private Integer priority;
    private String title;
    private String description;
    private LocalDateTime createdAt;
}
