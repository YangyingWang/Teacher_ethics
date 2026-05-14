package com.example.vo;

import lombok.Data;

@Data
public class AdminStatisticsSummaryVO {
    private Integer teacherTotal;
    private Integer assessmentCount;
    private Integer simulationCount;
    private Double avgAssessmentScore;
}
