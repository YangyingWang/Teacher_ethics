package com.example.dto.study.element;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class FavoritePreviewDTO {
    private Integer id;
    private String title;
    private String elementTypeName;
    private LocalDateTime collectedAt;
}
