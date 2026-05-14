package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class JudgmentQuestion {
    private Integer id;
    private Integer questionId;
    private Integer correctAnswer; // 0/1:contentReference[oaicite:13]{index=13}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
