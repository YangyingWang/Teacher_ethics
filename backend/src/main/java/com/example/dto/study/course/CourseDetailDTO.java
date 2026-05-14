package com.example.dto.study.course;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class CourseDetailDTO {
    private Integer id;
    private String title;
    private String description;
    private String cover;
    private String videoUrl;
    private Integer difficulty;
    private Integer categoryId;
    private String categoryName;
    private Integer duration;
    private Integer enrollment;
    private Integer status;
    private Integer progress;
    private Integer lastSec;
    private Integer studyTotal;
    private LocalDateTime lastStudyTime;
    private Boolean favorite;
}
