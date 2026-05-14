package com.example.dto.simulate;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class ChatReply {
    private Integer sessionId;
    private Integer aiSeq;
    private String aiMessage;
    private String phase;
    private Integer step;
}
