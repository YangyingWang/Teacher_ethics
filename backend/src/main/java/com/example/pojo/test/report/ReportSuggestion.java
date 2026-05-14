package com.example.pojo.test.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportSuggestion {
    private Integer id;
    private Integer reportId;
    private Integer dimensionId;
    private String title;
    private String content;
    private Integer priority; // 1高2中3低
    private LocalDateTime createdAt;
}
