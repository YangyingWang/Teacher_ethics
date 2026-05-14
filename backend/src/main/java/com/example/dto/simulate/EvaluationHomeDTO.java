package com.example.dto.simulate;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class EvaluationHomeDTO {
    private Integer overallScore = 0;
    private Integer decisionSpeed = 0;
    private Integer decisionQuality = 0;
    private Integer moralJudgment = 0;
    private Integer scenariosCompleted = 0;
    private Integer correctDecisionsRate = 0;
    private String ranking = "--";
    private Integer improvement = 0;
    private Integer percentile = 0;
    private Integer learningDays = 0;

    private DecisionPattern decisionPattern = new DecisionPattern();
    private List<DimensionItem> dimensions = new ArrayList<>();
    private List<ScenarioPerformanceItem> scenarioPerformances = new ArrayList<>();
    private List<StrengthItem> strengths = new ArrayList<>();
    private List<ImprovementItem> improvements = new ArrayList<>();
    private List<CriticalMomentItem> criticalMoments = new ArrayList<>();
    private List<String> focusAreas = new ArrayList<>();

    @Data
    public static class DimensionItem {
        private String name;
        private Integer score;
        private String color;
    }

    @Data
    public static class ScenarioPerformanceItem {
        private Integer evaluationId;
        private Integer sessionId;
        private String sessionTitle;
        private String completedAt;
        private Integer decisionTime;
        private Integer score;
        private List<String> keyDecisions = new ArrayList<>();
    }

    @Data
    public static class DecisionPattern {
        private String primaryStyle = "--";
        private Integer consistency = 0;
        private Integer avgTime = 0;
    }

    @Data
    public static class StrengthItem {
        private Integer id;
        private String title;
        private String description;
        private String evidence;
    }

    @Data
    public static class ImprovementItem {
        private Integer id;
        private String title;
        private String description;
        private String priority;
    }

    @Data
    public static class CriticalMomentItem {
        private Integer id;
        private String scenario;
        private String timestamp;
        private String type;
        private String impact;
        private String description;
        private String yourDecision;
        private String recommendedDecision;
        private String lesson;
    }
}
