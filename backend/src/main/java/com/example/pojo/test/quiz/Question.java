package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
//题目基类
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Question {
    private Integer id;            // 题目ID
    private Integer sceneId;       // 场景ID
    private Integer type;           // 0判断 1选择 2简答:contentReference[oaicite:9]{index=9}
    private String content;         //:contentReference[oaicite:10]{index=10}
    private String analysis;         //:contentReference[oaicite:11]{index=11}
    private Integer difficulty;    // 默认3:contentReference[oaicite:12]{index=12}
    private Integer score;         // 题目分值
    private LocalDateTime createdAt;    // 创建时间
    private LocalDateTime updatedAt;    // 更新时间
}
