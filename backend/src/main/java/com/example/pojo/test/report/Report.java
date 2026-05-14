package com.example.pojo.test.report;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Report {
    private Integer id;
    private Integer userId;
    private Integer questionnaireId;
    private Integer sceneId;
    private Integer status;         // 0生成中 1成功 2失败
    private Integer totalScore;
    private Integer userTotalScore;
    private Integer timeSpent;
    private Integer overallScore;   // 0-100
    private Integer overallLevel;   // 0优秀1良好2合格3需改进
    private Integer percentile;
    private String summary;
    private String rawJson;         // JSON字符串
    private String code;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
