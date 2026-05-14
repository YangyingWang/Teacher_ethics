package com.example.dto.test;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class TemplateDTO {
    private String title;
    private List<Q> questions;

    @Data
    public static class Q {
        private Integer type; // 0/1/2
        private String content;
        private String analysis;
        private Integer difficulty;
        private Integer score;

        // 判断
        private Boolean correctAnswer;

        // 选择
        private String optionA;
        private String optionB;
        private String optionC;
        private String optionD;
        private Boolean isMultiple;
        private List<String> correctAnswers;

        // 简答
        private String reference;
        private String keyword;
        /**
         * dimensions:
         * [
         *   { "dimensionId": 1, "weight": 0.7 },
         *   { "dimensionId": 3, "weight": 0.3 }
         * ]
         */
        private List<QD> dimensions;
    }

    @Data
    public static class QD {
        private Integer dimensionId;
        private BigDecimal weight;
    }
}
