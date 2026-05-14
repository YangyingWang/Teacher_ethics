package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class TestTemplate {
    private Integer id;
    private Integer sceneId;
    private String title;
    private String questionSequence; // JSON
    private Integer totalScore;
    private Integer totalCount;
    private String rawJson;          // JSON string
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
