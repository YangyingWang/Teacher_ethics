package com.example.pojo.test;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class Scene {
    @NotNull
    private Integer id;
    private String title;
    private String description;
    private String imgUrl;
    private Integer difficulty;
    private Integer estimatedTime;
    private Integer participants;
    private Integer categoryId;
    private String focus;
    private String analysis;
    private String correctApproach;
    private String incorrectApproach;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @JsonIgnore
    public String getFullImageUrl() {
        if (imgUrl == null || imgUrl.isEmpty()) {
            return null;
        }

        // 如果是相对路径，添加基础URL
        if (!imgUrl.startsWith("http")) {
            String baseUrl = "http://localhost:8080"; // 应该从配置读取
            return baseUrl + imgUrl;
        }
        return imgUrl;
    }
}
