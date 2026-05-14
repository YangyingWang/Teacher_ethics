package com.example.pojo.simulate.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class ChatSession {
    private Integer id;
    private Integer userId;
    private Integer sceneCategoryId;

    private String title;
    private String status;  // active/completed/archived
    private String phase;   // init/scenario/dialogue/evaluation
    private Integer step;
    private Integer maxSteps;
    private String scenario;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
