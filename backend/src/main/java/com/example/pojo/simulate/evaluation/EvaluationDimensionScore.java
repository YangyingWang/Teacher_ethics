package com.example.pojo.simulate.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationDimensionScore {
    private Integer id;
    private Integer dimensionId;
    private Integer evaluationId;
    private Integer score;
    private BigDecimal weight;
    private String comment;
    private LocalDateTime createdAt;
}
