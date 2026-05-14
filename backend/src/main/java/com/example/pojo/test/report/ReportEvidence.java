package com.example.pojo.test.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportEvidence {
    private Integer id;
    private Integer itemId;
    private Integer reportId;
    private Integer questionId;
    private String reason;
    private LocalDateTime createdAt;
}
