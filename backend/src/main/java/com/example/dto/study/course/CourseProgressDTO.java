package com.example.dto.study.course;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseProgressDTO {
    private Integer courseId;
    private Integer lastSec;
    private Integer studySec;
    private BigDecimal progressPercent;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
