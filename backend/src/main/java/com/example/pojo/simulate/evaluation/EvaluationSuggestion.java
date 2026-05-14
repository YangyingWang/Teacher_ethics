package com.example.pojo.simulate.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationSuggestion {
    private Integer id;
    private Integer evaluationId;
    private Integer dimensionId;

    private Integer type; // 0整体建议 1维度建议 2针对性建议
    private String title;
    private String content;
    private Integer priority; // 1高 2中 3低

    private LocalDateTime createdAt;
}
