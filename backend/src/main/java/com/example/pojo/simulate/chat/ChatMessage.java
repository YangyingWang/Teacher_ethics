package com.example.pojo.simulate.chat;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ChatMessage {
    private Integer id;
    private Integer sessionId;

    private String role;         // user/assistant/system
    private String content;
    private String contentFormat; // markdown/plain

    private Integer seq;
    private BigDecimal temperature; // decimal(4,3) 例如 0.700

    private LocalDateTime createdAt;
}
