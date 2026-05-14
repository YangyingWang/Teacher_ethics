package com.example.pojo.study.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseLearning {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private Integer status;
    private BigDecimal progressPercent;
    private Integer lastSec;
    private Integer studyTotal;
    private LocalDateTime lastTime;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
