package com.example.dto.assessment;

import lombok.Data;

@Data
public class AssessmentModuleStatsDTO {
    private Integer activityCount;
    private Integer avgScore;
    private Integer bestScore;
    private Integer avgProgress;
    private Integer activeDays;
    private Integer highRiskRate;
}
