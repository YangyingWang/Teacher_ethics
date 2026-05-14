package com.example.dto.study.element;

import lombok.Data;

@Data
public class SearchQueryDTO {
    private String keyword;
    private Integer disciplineId;
    private Integer elementTypeId;
    private Integer courseTypeId;
    private Integer difficulty;
    private String sortBy = "relevance";
    private Integer pageNum = 1;
    private Integer pageSize = 12;
    private Boolean onlyFavorite = false;
}
