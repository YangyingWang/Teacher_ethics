package com.example.pojo.study.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseUserLearn {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private LocalDate learnDate;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private Integer studySec;
    private BigDecimal progressBefore;
    private BigDecimal progressAfter;
    private LocalDateTime createdAt;
}
