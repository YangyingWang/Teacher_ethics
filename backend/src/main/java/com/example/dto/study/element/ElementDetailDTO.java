package com.example.dto.study.element;

import lombok.Data;

import java.util.List;

@Data
public class ElementDetailDTO {
    private Integer id;
    private String title;
    private String summary;
    private String content;
    private Integer difficulty;
    private String ideologyType;
    private List<String> disciplines;
    private List<String> courseTypes;
    private List<String> suitableCourses;
    private List<String> keywords;
    private Integer viewCount;
    private Integer favoriteCount;
    private Integer useCount;
    private Integer popularity;
    private Boolean collected;
}
