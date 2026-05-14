package com.example.pojo.simulate.evaluation;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationItem {
    private Integer id;
    private Integer evaluationId;
    private Integer type; // 0优势 1风险 2关键节点
    private Integer dimensionId;
    private String content;
    private Integer roundNo;
    private Integer level; // 1高 2中 3低
    private LocalDateTime createdAt;
}
