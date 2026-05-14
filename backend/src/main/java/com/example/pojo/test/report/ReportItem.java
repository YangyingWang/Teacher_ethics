package com.example.pojo.test.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportItem {
    private Integer id;
    private Integer reportId;
    private Integer type; // 1优势 2风险
    private String content;
    private Integer level; // 1高2中3低
    private LocalDateTime createdAt;
}
