package com.example.pojo.simulate.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EvaluationDimension {
    private Integer id;
    private String name;
    private Integer type; // 0主维度 1过程维度
    private String description;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
