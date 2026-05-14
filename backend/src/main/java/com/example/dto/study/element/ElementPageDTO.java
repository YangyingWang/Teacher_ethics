package com.example.dto.study.element;

import lombok.Data;

import java.util.List;

@Data
public class ElementPageDTO {
    private Long total;
    private Integer pageNum;
    private Integer pageSize;
    private List<ElementCardDTO> list;
}
