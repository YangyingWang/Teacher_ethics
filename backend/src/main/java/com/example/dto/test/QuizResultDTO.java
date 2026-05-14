package com.example.dto.test;

import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class QuizResultDTO {
    private Integer qnId;
    private String title;
    private Integer totalScore;
    private Integer userTotalScore;

    private Integer timeSpent;
    private LocalDateTime submittedAt;
    private List<QuestionResult> questionResults;

    @Data
    public static class QuestionResult {
        private Integer id;
        private Integer type;
        private String content;
        private Integer difficulty;
        private Integer score;
        private String analysis; // 解析（只在结果页展示）

        // 选择题选项
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private Boolean isMultiple;

        private Object userAnswer;
        private Integer userScore;
        private Boolean isMarked;

        // 正确答案（判断/选择）
        private Object correctAnswer;
        // 简答题参考
        private String reference;
        private String keyword;
    }
}
