package com.example.pojo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Assessment {
    private Integer id;
    private Integer userId;
    private LocalDate recordDate;

    private Integer overallScore;
    private Integer overallLevel;

    private Integer learningScore;
    private Integer abilityScore;
    private Integer governanceScore;

    private Integer monthlyChange;
    private Integer lastDays;
    private Integer totalActivities;
    private Integer improvementRate;
    private Integer ranking;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
