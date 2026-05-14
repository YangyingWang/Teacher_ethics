package com.example.pojo.test.quiz;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class QuestionDimension {
    private Integer id;
    private Integer questionId;
    private Integer dimensionId;
    private BigDecimal weight; // 0~1
    private LocalDateTime createdAt;
}
