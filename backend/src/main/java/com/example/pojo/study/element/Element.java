package com.example.pojo.study.element;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Element {
    private Integer id;
    private String title;
    private String summary;
    private String content;
    private Integer elementTypeId;
    private Integer difficulty;
    private String keywords;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer useCount;
    private BigDecimal hotScore;
    private Integer status;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
