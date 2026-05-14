package com.example.dto.test;

import lombok.Data;

import java.util.List;

@Data
public class SubmitRequest {
    private Integer timeSpent; // 秒
    private List<Answer> answers;

    @Data
    public static class Answer {
        private Integer questionId;
        // 判断：true/false；选择：["A"] 或 ["A","C"]；简答：字符串
        private Object userAnswer;
        private Boolean isMarked;
    }
}
