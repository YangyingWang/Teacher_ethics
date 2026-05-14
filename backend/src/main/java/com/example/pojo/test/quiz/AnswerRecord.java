package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AnswerRecord {
    private Integer id;            // 记录ID
    private Integer questionnaireId; // 答卷ID
    private Integer questionId;    // 题目ID
    private String userAnswer; // JSON:contentReference[oaicite:21]{index=21}
    private Integer userScore;
    private Integer isMarked;   // 0/1:contentReference[oaicite:22]{index=22}
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
