package com.example.pojo.study.course;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CourseFavorite {
    private Integer id;
    private Integer userId;
    private Integer courseId;
    private LocalDateTime createdAt;
}
