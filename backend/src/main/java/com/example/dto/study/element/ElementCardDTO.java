package com.example.dto.study.element;

import lombok.Data;

import java.util.List;

@Data
public class ElementCardDTO {
    private Integer id;
    private String title;
    private String description;
    private Integer difficulty;
    private String ideologyType;
    private List<String> suitableCourses;
    private List<String> keywords;
    private Double recommendationScore;
    private Boolean collected;
    private Integer popularity;
}
