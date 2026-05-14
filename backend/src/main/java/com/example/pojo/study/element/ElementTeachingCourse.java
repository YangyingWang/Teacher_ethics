package com.example.pojo.study.element;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ElementTeachingCourse {
    private Integer id;
    private Integer elementId;
    private Integer teachingCourseId;
    private LocalDateTime createdAt;
}
