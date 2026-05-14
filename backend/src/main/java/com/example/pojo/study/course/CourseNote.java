package com.example.pojo.study.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseNote {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private String content;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
