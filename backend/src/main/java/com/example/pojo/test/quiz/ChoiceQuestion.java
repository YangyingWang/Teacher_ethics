package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChoiceQuestion {
    private Integer id;
    private Integer questionId;
    private String optionA;     // option_A:contentReference[oaicite:14]{index=14}
    private String optionB;
    private String optionC;
    private String optionD;
    private Integer isMultiple; // 0/1:contentReference[oaicite:15]{index=15}
    private String correctAnswer; // JSON: ["A","C"]:contentReference[oaicite:16]{index=16}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
