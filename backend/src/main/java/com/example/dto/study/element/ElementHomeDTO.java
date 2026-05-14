package com.example.dto.study.element;

import lombok.Data;

import java.util.List;

@Data
public class ElementHomeDTO {
    private List<OptionDTO> elementTypes;
    private List<OptionDTO> disciplines;
    private List<OptionDTO> courseTypes;
    private List<OptionDTO> teachingCourses;
    private List<String> suggestedKeywords;
    private List<ElementCardDTO> recommendations;
    private List<FavoritePreviewDTO> favoriteList;
    private Stats stats;

    @Data
    public static class Stats {
        private Integer totalElements;
        private Integer totalCourses;
        private Integer favoriteCount;
    }
}
