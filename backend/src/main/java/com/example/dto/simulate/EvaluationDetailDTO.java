package com.example.dto.simulate;

import lombok.Data;

import java.math.BigDecimal;
import java.util.List;

@Data
public class EvaluationDetailDTO {
    private Integer evaluationId;
    private Integer sessionId;
    private Integer sceneCategoryId;

    private Integer overallScore;
    private Integer overallLevel; // 0优秀 1良好 2合格 3需改进
    private String summary;
    private Integer style; // 0稳健型 1权衡型 2激进型
    private Integer riskLevel; // 1高 2中 3低

    private List<DimScore> dimScores;
    private List<ItemWithEvidences> strengths;
    private List<ItemWithEvidences> risks;
    private List<ItemWithEvidences> criticalMoments;
    private List<Suggestion> suggestions;

    @Data
    public static class DimScore {
        private Integer id;
        private Integer score; // 0-100
        private BigDecimal weight; // 0-1
        private String comment;
    }

    @Data
    public static class ItemWithEvidences {
        private Integer id;
        private Integer dimensionId;
        private String content;
        private Integer level;
        private List<Evidence> evidences;
    }

    @Data
    public static class Evidence {
        private Integer messageId;
        private String reason;
    }

    @Data
    public static class Suggestion {
        private Integer id;
        private Integer dimensionId;
        private String title;
        private String content;
        private Integer priority;
    }
}
