package com.example.dto.test;

import lombok.Data;

import java.util.List;

@Data
public class ReportDetailDTO {
    private Integer reportId;
    private Integer qnId;
    private Integer sceneId;

    private Integer totalScore;
    private Integer userTotalScore;
    private Integer timeSpent;

    private Integer overallScore;
    private Integer overallLevel;
    private String summary;
    private String code;

    private List<DimScore> dimScores;
    private List<ItemWithEvidences> strengths;
    private List<ItemWithEvidences> risks;
    private List<Suggestion> suggestions;

    @Data
    public static class DimScore {
        private Integer id;
        private Integer score; // 0-100
        private Integer questionCount;
        private Integer wrongCount;
        private Integer lowCount;
    }

    @Data
    public static class ItemWithEvidences {
        private Integer id;
        private String content;
        private Integer level;
        private List<Evidence> evidences;
    }

    @Data
    public static class Evidence {
        private Integer questionId;
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
