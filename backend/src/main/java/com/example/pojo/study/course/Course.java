package com.example.pojo.study.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Course {
    private Integer id;
    private Integer categoryId;
    private String title;
    private String description;
    private String coverUrl;
    private String videoUrl;
    private Integer difficulty;
    private Integer duration;
    private Integer teacherType;
    private Integer enrollmentCount;
    private BigDecimal hotScore;
    private Integer isFeatured;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
