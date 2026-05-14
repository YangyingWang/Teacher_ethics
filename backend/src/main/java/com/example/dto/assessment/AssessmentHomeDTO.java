package com.example.dto.assessment;

import lombok.Data;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
public class AssessmentHomeDTO {
    private Integer lastUpdateDays;
    private Integer totalActivities;
    private Integer improvementRate;
    private Integer rankingPercentile;

    private Integer overallScore;
    private String overallGrade;
    private Integer learningInvestment;
    private Integer abilityPerformance;
    private Integer governanceLevel;
    private Integer monthlyChange;

    private List<DimensionScore> dimensions;
    private Map<String, ModulePanel> modules = new LinkedHashMap<>();
    private AiAdvice aiAdvice;
    private List<ComparisonInsight> comparisonInsights;

    /**
     * 前端趋势图可直接使用：
     * {
     *   "learning": [{label:"1月",score:81}, ...],
     *   "ability":  [{label:"1月",score:78}, ...],
     *   "governance":[{label:"1月",score:75}, ...]
     * }
     */
    private Map<String, List<TrendPoint>> trendData = new LinkedHashMap<>();

    /**
     * 对比图额外数据：个人分数 + 对比对象均值
     */
    private List<DimensionCompare> comparisonDimensions;

    @Data
    public static class DimensionScore {
        private String name;
        private Integer score;
    }

    @Data
    public static class ModulePanel {
        private String name;
        private String description;
        private Integer score;
        private List<Metric> metrics;
    }

    @Data
    public static class Metric {
        private String name;
        private String value;
        private Integer percentage;
        private String target;
        private Integer trend;
    }

    @Data
    public static class AiAdvice {
        private String summary;
        private List<AdviceItem> suggestions;
    }

    @Data
    public static class AdviceItem {
        private String priority;
        private String title;
        private String description;
    }

    @Data
    public static class ComparisonInsight {
        private Integer id;
        private String type;
        private String title;
        private String detail;
    }

    @Data
    public static class TrendPoint {
        private String label;
        private Integer score;
    }

    @Data
    public static class DimensionCompare {
        private String name;
        private Integer personalScore;
        private Integer compareScore;
    }
}
