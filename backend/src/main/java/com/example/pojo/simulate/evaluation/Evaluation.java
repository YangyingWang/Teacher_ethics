package com.example.pojo.simulate.evaluation;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class Evaluation {
    private Integer id;
    private Integer sessionId;
    private Integer userId;

    private Integer status; // 0生成中 1成功 2失败
    private Integer overallScore;
    private Integer overallLevel; // 0优秀 1良好 2合格 3需改进
    private Integer percentile;
    private String summary;
    private Integer style; // 0稳健型 1权衡型 2激进型
    private Integer riskLevel; // 1高 2中 3低

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
