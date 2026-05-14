package com.example.pojo.test.quiz;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class EssayQuestion {
    private Integer id;
    private Integer questionId;
    private String reference; //:contentReference[oaicite:17]{index=17}
    private String keyword; //:contentReference[oaicite:18]{index=18}
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
