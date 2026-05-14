package com.example.dto.simulate;

import lombok.Data;

@Data
public class ChatRequest {
    private Integer sessionId;
    private String message;   // 用户输入
}
