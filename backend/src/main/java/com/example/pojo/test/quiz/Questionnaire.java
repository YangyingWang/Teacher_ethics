package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Questionnaire {
    private Integer id;            // 答卷ID
    private Integer userId;        // 用户ID
    private Integer sceneId;       // 场景ID
    private String title;      // 试卷标题
    private String questionSequence; // JSON:contentReference[oaicite:19]{index=19}

    private Integer totalScore;
    private Integer totalCount;
    private Integer userTotalScore;
    private Integer status;     // 0进行中 1已完成 2已放弃:contentReference[oaicite:20]{index=20}
    private LocalDateTime startedAt;    // 开始时间
    private LocalDateTime submittedAt;  // 提交时间
    private Integer timeSpent; // 用时（秒）
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
