package com.example.dto.test;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuestionDTO {
    private Integer id;
    private Integer type;
    private String content;
    private Integer difficulty;
    private Integer score;

    // 选择题用
    private String optionA;
    private String optionB;
    private String optionC;
    private String optionD;
    private Boolean isMultiple;
}
