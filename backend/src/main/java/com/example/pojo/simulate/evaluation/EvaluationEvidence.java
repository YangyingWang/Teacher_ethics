package com.example.pojo.simulate.evaluation;
import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
@Data
@AllArgsConstructor
@NoArgsConstructor
public class EvaluationEvidence {
    private Integer id;
    private Integer itemId;
    private Integer evaluationId;
    private Integer messageId;
    private String reason;
    private LocalDateTime createdAt;
}
