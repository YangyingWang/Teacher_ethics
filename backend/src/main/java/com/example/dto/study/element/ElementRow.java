package com.example.dto.study.element;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class ElementRow {
    private Integer id;
    private String title;
    private String summary;
    private String content;
    private Integer elementTypeId;
    private String elementTypeName;
    private Integer difficulty;
    private String keywords;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer useCount;
    private BigDecimal hotScore;
    private LocalDateTime createdAt;

    private Integer collectedFlag;
    private Integer teacherMatchedFlag;
    private Integer disciplineMatchedFlag;
}
