package com.example.dto.test;

import lombok.Data;

@Data
public class ReportQuizFactDTO {
    private Integer qId;
    private Integer type;
    private String content;
    private String analysis;

    private Integer fullScore;
    private Integer userScore;
    private String userAnswer; // answer_records.user_answer（JSON字符串）

    // choice
    private Boolean isMultiple;
    private String correctAnswers; // choice_questions.correct_answer（JSON字符串）

    // judgment
    private Integer correctAnswerTf; // judgment_questions.correct_answer（0/1）

    // essay
    private String reference;
    private String keyword;
}
