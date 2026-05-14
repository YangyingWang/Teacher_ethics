package com.example.vo;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class AdminSummaryVO {
    private Integer teacherTotal;
    private Integer courseLearnerCount;
    private Integer simulationParticipantCount;
    private Integer evaluationCount;
    private Integer assessmentCount;
    private BigDecimal avgAssessmentScore;
}
