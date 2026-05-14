package com.example.pojo.test.report;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class ReportDimension {
    private Integer id;
    private String name;
    private String description;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
