package com.example.pojo.test.report;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ReportDimensionScore {
    private Integer id;
    private Integer dimensionId;
    private Integer reportId;
    private Integer score; // 0-100
    private Integer rawScore;
    private Integer rawTotal;
    private Integer questionCount;
    private BigDecimal weight; // 0-1
    private Integer wrongCount;
    private Integer lowCount;
    private LocalDateTime createdAt;
}
