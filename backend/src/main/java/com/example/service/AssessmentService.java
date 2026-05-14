package com.example.service;

import com.alibaba.excel.EasyExcel;
import com.alibaba.excel.ExcelWriter;
import com.alibaba.excel.write.metadata.WriteSheet;
import com.alibaba.excel.write.style.column.LongestMatchColumnWidthStyleStrategy;
import com.example.dto.assessment.AssessmentAdviceRow;
import com.example.dto.assessment.AssessmentDimensionRow;
import com.example.dto.assessment.AssessmentHomeDTO;
import com.example.mapper.assessment.AssessmentAnalyticsMapper;
import com.example.mapper.assessment.AssessmentMapper;
import com.example.pojo.Assessment;
import com.example.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Month;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class AssessmentService {
    @Autowired
    private AssessmentMapper assessmentMapper;
    @Autowired
    private AssessmentAnalyticsMapper analyticsMapper;

    public AssessmentHomeDTO getHomeData(String timeRange, String trendRange, String comparisonTarget) {
        Integer userId = currentUserId();

        LocalDate today = LocalDate.now();
        LocalDate homeStartDate = resolveHomeStartDate(timeRange, today);
        Assessment latest = assessmentMapper.selectLatestByUserIdAndRange(userId, homeStartDate, today);
        if (latest == null) {
            latest = refreshAssessmentSnapshot(userId, today);
        }

        AssessmentHomeDTO dto = new AssessmentHomeDTO();
        dto.setLastUpdateDays(latest.getLastDays() == null ? 0 : latest.getLastDays());
        dto.setTotalActivities(nullSafe(latest.getTotalActivities()));
        dto.setImprovementRate(nullSafe(latest.getImprovementRate()));
        dto.setRankingPercentile(nullSafe(latest.getRanking()));
        dto.setOverallScore(nullSafe(latest.getOverallScore()));
        dto.setOverallGrade(levelText(latest.getOverallLevel()));
        dto.setLearningInvestment(nullSafe(latest.getLearningScore()));
        dto.setAbilityPerformance(nullSafe(latest.getAbilityScore()));
        dto.setGovernanceLevel(nullSafe(latest.getGovernanceScore()));
        dto.setMonthlyChange(nullSafe(latest.getMonthlyChange()));

        dto.setDimensions(buildDimensions(userId, homeStartDate, today));
        dto.setModules(buildModules(userId, homeStartDate, today, latest));
        dto.setAiAdvice(buildAiAdvice(latest, dto.getDimensions(), userId));
        dto.setComparisonDimensions(buildComparisonDimensions(dto.getDimensions(), comparisonTarget, homeStartDate, today));
        dto.setComparisonInsights(buildComparisonInsights(dto.getComparisonDimensions()));
        dto.setTrendData(buildTrendData(userId, trendRange, today));

        return dto;
    }

    @Transactional
    public AssessmentHomeDTO refreshHomeData(String timeRange, String trendRange, String comparisonTarget) {
        Integer userId = currentUserId();
        refreshAssessmentSnapshot(userId, LocalDate.now());
        return getHomeData(timeRange, trendRange, comparisonTarget);
    }

    @Transactional
    public Assessment refreshAssessmentSnapshot(Integer userId, LocalDate recordDate) {
        LocalDate calcStartDate = recordDate.minusMonths(3);

        Integer learningScore = calcLearningScore(userId, calcStartDate, recordDate);
        Integer abilityScore = calcAbilityScore(userId, calcStartDate, recordDate);
        Integer governanceScore = calcGovernanceScore(userId, calcStartDate, recordDate);
        Integer overallScore = calcOverallScore(learningScore, abilityScore, governanceScore);

        Assessment previous = assessmentMapper.selectPreviousByUserId(userId, recordDate);

        Assessment assessment = new Assessment();
        assessment.setUserId(userId);
        assessment.setRecordDate(recordDate);
        assessment.setLearningScore(learningScore);
        assessment.setAbilityScore(abilityScore);
        assessment.setGovernanceScore(governanceScore);
        assessment.setOverallScore(overallScore);
        assessment.setOverallLevel(calcLevel(overallScore));

        int learningActivities = nullSafe(analyticsMapper.countLearningActivities(userId, calcStartDate, recordDate));
        int abilityActivities = nullSafe(analyticsMapper.countAbilityActivities(userId, calcStartDate, recordDate));
        int governanceActivities = nullSafe(analyticsMapper.countGovernanceActivities(userId, calcStartDate, recordDate));
        assessment.setTotalActivities(learningActivities + abilityActivities + governanceActivities);

        if (previous != null) {
            assessment.setMonthlyChange(overallScore - nullSafe(previous.getOverallScore()));
            assessment.setLastDays((int) (recordDate.toEpochDay() - previous.getRecordDate().toEpochDay()));

            int prev = nullSafe(previous.getOverallScore());
            if (prev > 0) {
                assessment.setImprovementRate((int) Math.round((overallScore - prev) * 100.0 / prev));
            } else {
                assessment.setImprovementRate(0);
            }
        } else {
            assessment.setMonthlyChange(0);
            assessment.setLastDays(0);
            assessment.setImprovementRate(0);
        }

        assessment.setRanking(0);
        assessmentMapper.upsert(assessment);

        Integer total = nullSafe(assessmentMapper.countByRecordDate(recordDate));
        Integer better = nullSafe(assessmentMapper.countBetterByRecordDate(recordDate, overallScore));
        int ranking = total <= 0 ? 0 : (int) Math.round(better * 100.0 / total);
        assessment.setRanking(ranking);

        assessmentMapper.upsert(assessment);
        return assessmentMapper.selectByUserIdAndDate(userId, recordDate);
    }

    public Map<String, Object> generateReport(String timeRange) {
        AssessmentHomeDTO home = getHomeData(timeRange, "6m", "department");

        Map<String, Object> out = new LinkedHashMap<>();
        out.put("overallScore", home.getOverallScore());
        out.put("overallGrade", home.getOverallGrade());
        out.put("learningScore", home.getLearningInvestment());
        out.put("abilityScore", home.getAbilityPerformance());
        out.put("governanceScore", home.getGovernanceLevel());
        out.put("dimensions", home.getDimensions());
        out.put("aiAdvice", home.getAiAdvice());
        out.put("generatedAt", new Date());
        return out;
    }

    public void exportExcel(String timeRange,
                            String trendRange,
                            String comparisonTarget,
                            HttpServletResponse response) throws Exception {
        AssessmentHomeDTO home = getHomeData(timeRange, trendRange, comparisonTarget);

        String fileName = "assessment_" + LocalDate.now() + ".xlsx";
        String encodedFileName = URLEncoder.encode(fileName, StandardCharsets.UTF_8).replaceAll("\\+", "%20");

        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setCharacterEncoding("UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFileName);

        ExcelWriter excelWriter = null;
        try {
            excelWriter = EasyExcel.write(response.getOutputStream())
                    .autoCloseStream(false)
                    .registerWriteHandler(new LongestMatchColumnWidthStyleStrategy())
                    .build();

            WriteSheet summarySheet = EasyExcel.writerSheet("综合评估")
                    .head(buildHead("模块", "指标", "值"))
                    .build();
            excelWriter.write(buildSummaryRows(home), summarySheet);

            WriteSheet moduleSheet = EasyExcel.writerSheet("模块表现")
                    .head(buildHead("模块", "说明", "得分"))
                    .build();
            excelWriter.write(buildModuleRows(home), moduleSheet);

            WriteSheet metricSheet = EasyExcel.writerSheet("模块指标")
                    .head(buildHead("模块", "指标名", "指标值", "百分比", "目标值", "趋势"))
                    .build();
            excelWriter.write(buildMetricRows(home), metricSheet);

            WriteSheet dimSheet = EasyExcel.writerSheet("能力维度")
                    .head(buildHead("维度名称", "个人得分"))
                    .build();
            excelWriter.write(buildDimensionRows(home), dimSheet);

            WriteSheet compareSheet = EasyExcel.writerSheet("对比分析")
                    .head(buildHead("维度名称", "个人得分", "对比对象得分"))
                    .build();
            excelWriter.write(buildComparisonRows(home), compareSheet);

            WriteSheet trendSheet = EasyExcel.writerSheet("成长趋势")
                    .head(buildHead("模块", "时间", "得分"))
                    .build();
            excelWriter.write(buildTrendRows(home), trendSheet);

            WriteSheet adviceSheet = EasyExcel.writerSheet("AI建议")
                    .head(buildHead("类型", "标题", "内容", "优先级"))
                    .build();
            excelWriter.write(buildAdviceRows(home), adviceSheet);

            WriteSheet insightSheet = EasyExcel.writerSheet("对比结论")
                    .head(buildHead("标题", "详情"))
                    .build();
            excelWriter.write(buildInsightRows(home), insightSheet);

        } finally {
            if (excelWriter != null) {
                excelWriter.finish();
            }
        }
    }

    private Map<String, AssessmentHomeDTO.ModulePanel> buildModules(Integer userId, LocalDate startDate, LocalDate endDate, Assessment latest) {
        Map<String, AssessmentHomeDTO.ModulePanel> modules = new LinkedHashMap<>();

        AssessmentHomeDTO.ModulePanel learning = new AssessmentHomeDTO.ModulePanel();
        learning.setName("学习筑基");
        learning.setDescription("师德基础知识学习与课程思政能力培养");
        learning.setScore(nullSafe(latest.getLearningScore()));
        learning.setMetrics(buildLearningMetrics(userId, startDate, endDate));
        modules.put("learning", learning);

        AssessmentHomeDTO.ModulePanel ability = new AssessmentHomeDTO.ModulePanel();
        ability.setName("能力提升");
        ability.setDescription("师德情景判断与决策能力训练");
        ability.setScore(nullSafe(latest.getAbilityScore()));
        ability.setMetrics(buildAbilityMetrics(userId, startDate, endDate));
        modules.put("ability", ability);

        AssessmentHomeDTO.ModulePanel governance = new AssessmentHomeDTO.ModulePanel();
        governance.setName("治理研修");
        governance.setDescription("师德事件处理与治理能力提升");
        governance.setScore(nullSafe(latest.getGovernanceScore()));
        governance.setMetrics(buildGovernanceMetrics(userId, startDate, endDate));
        modules.put("governance", governance);

        return modules;
    }

    private List<AssessmentHomeDTO.Metric> buildLearningMetrics(Integer userId, LocalDate startDate, LocalDate endDate) {
        int sessionCount = nullSafe(analyticsMapper.countLearningActivities(userId, startDate, endDate));
        int studySeconds = nullSafe(analyticsMapper.sumLearningStudySeconds(userId, startDate, endDate));
        int completionRate = calcLearningCompletionRate(userId, startDate, endDate);

        int studyHoursTenths = (int) Math.round(studySeconds / 360.0);

        List<AssessmentHomeDTO.Metric> list = new ArrayList<>();
        list.add(metric(
                "学习会话数",
                sessionCount + "次",
                percentOfTarget(sessionCount, 10),
                "10次",
                estimateTrend(sessionCount, 10)
        ));
        list.add(metric(
                "累计学习时长",
                formatHours(studySeconds) + "小时",
                percentOfTarget(studyHoursTenths, 80),
                "8小时",
                estimateTrend(studyHoursTenths, 80)
        ));
        list.add(metric(
                "课程完成率",
                completionRate + "%",
                completionRate,
                "70%",
                estimateTrend(completionRate, 70)
        ));
        return list;
    }

    private List<AssessmentHomeDTO.Metric> buildAbilityMetrics(Integer userId, LocalDate startDate, LocalDate endDate) {
        Integer count = nullSafe(analyticsMapper.countAbilityActivities(userId, startDate, endDate));
        Integer avgScore = nullSafe(analyticsMapper.avgAbilityScore(userId, startDate, endDate));
        Integer bestScore = nullSafe(analyticsMapper.maxAbilityScore(userId, startDate, endDate));

        List<AssessmentHomeDTO.Metric> list = new ArrayList<>();
        list.add(metric("测试完成数", count + "次", Math.min(count * 10, 100), "10次", estimateTrend(avgScore, 80)));
        list.add(metric("平均得分", avgScore + "分", avgScore, "90分", estimateTrend(avgScore, 90)));
        list.add(metric("最佳表现", bestScore + "分", bestScore, "95分", estimateTrend(bestScore, 95)));
        return list;
    }

    private List<AssessmentHomeDTO.Metric> buildGovernanceMetrics(Integer userId, LocalDate startDate, LocalDate endDate) {
        Integer count = nullSafe(analyticsMapper.countGovernanceActivities(userId, startDate, endDate));
        Integer avgScore = nullSafe(analyticsMapper.avgGovernanceScore(userId, startDate, endDate));
        Integer highRiskRate = nullSafe(analyticsMapper.governanceHighRiskRate(userId, startDate, endDate));

        List<AssessmentHomeDTO.Metric> list = new ArrayList<>();
        list.add(metric("沙盘演练", count + "次", Math.min(count * 12, 100), "8次", estimateTrend(avgScore, 80)));
        list.add(metric("平均决策得分", avgScore + "分", avgScore, "85分", estimateTrend(avgScore, 85)));
        list.add(metric("高风险占比", highRiskRate + "%", Math.max(0, 100 - highRiskRate), "≤20%", highRiskRate <= 20 ? 6 : -6));
        return list;
    }

    private List<AssessmentHomeDTO.DimensionScore> buildDimensions(Integer userId, LocalDate startDate, LocalDate endDate) {
        List<AssessmentDimensionRow> rows = analyticsMapper.listPersonalDimensions(userId, startDate, endDate);
        return rows.stream().map(row -> {
            AssessmentHomeDTO.DimensionScore item = new AssessmentHomeDTO.DimensionScore();
            item.setName(row.getName());
            item.setScore(nullSafe(row.getScore()));
            return item;
        }).collect(Collectors.toList());
    }

    private List<AssessmentHomeDTO.DimensionCompare> buildComparisonDimensions(List<AssessmentHomeDTO.DimensionScore> personal,
                                                                               String comparisonTarget, LocalDate startDate, LocalDate endDate) {
        List<AssessmentDimensionRow> compareRows = analyticsMapper.listCompareDimensions(normalizeComparisonTarget(comparisonTarget), startDate, endDate);
        Map<String, Integer> compareMap = compareRows.stream()
                .collect(Collectors.toMap(AssessmentDimensionRow::getName,
                        x -> nullSafe(x.getScore()),
                        (a, b) -> a,
                        LinkedHashMap::new));

        List<AssessmentHomeDTO.DimensionCompare> list = new ArrayList<>();
        for (AssessmentHomeDTO.DimensionScore item : personal) {
            AssessmentHomeDTO.DimensionCompare d = new AssessmentHomeDTO.DimensionCompare();
            d.setName(item.getName());
            d.setPersonalScore(item.getScore());
            d.setCompareScore(compareMap.getOrDefault(item.getName(), 80));
            list.add(d);
        }
        return list;
    }

    private AssessmentHomeDTO.AiAdvice buildAiAdvice(Assessment latest, List<AssessmentHomeDTO.DimensionScore> dimensions, Integer userId) {
        AssessmentHomeDTO.AiAdvice aiAdvice = new AssessmentHomeDTO.AiAdvice();

        String weakest = dimensions.isEmpty()
                ? "综合研修能力"
                : dimensions.stream().min(Comparator.comparingInt(AssessmentHomeDTO.DimensionScore::getScore))
                .map(AssessmentHomeDTO.DimensionScore::getName).orElse("综合研修能力");
        String strongest = dimensions.isEmpty()
                ? "师德基础认知"
                : dimensions.stream().max(Comparator.comparingInt(AssessmentHomeDTO.DimensionScore::getScore))
                .map(AssessmentHomeDTO.DimensionScore::getName).orElse("师德基础认知");

        aiAdvice.setSummary(String.format(
                "您当前综合评估得分为%d分，整体等级为%s。其中“%s”表现相对较好，“%s”仍有提升空间。建议继续保持优势能力，同时围绕薄弱维度加强专题学习、情景测试与沙盘推演训练。",
                nullSafe(latest.getOverallScore()), levelText(latest.getOverallLevel()), strongest, weakest
        ));

        List<AssessmentAdviceRow> adviceRows = analyticsMapper.listLatestAdvice(userId, 3);
        List<AssessmentHomeDTO.AdviceItem> suggestions = adviceRows.stream().map(row -> {
            AssessmentHomeDTO.AdviceItem item = new AssessmentHomeDTO.AdviceItem();
            item.setPriority(priorityText(row.getPriority()));
            item.setTitle(row.getTitle());
            item.setDescription(row.getDescription());
            return item;
        }).collect(Collectors.toList());

        if (suggestions.isEmpty()) {
            AssessmentHomeDTO.AdviceItem fallback = new AssessmentHomeDTO.AdviceItem();
            fallback.setPriority("中");
            fallback.setTitle("加强薄弱维度专项训练");
            fallback.setDescription("系统暂未检索到历史建议，建议优先围绕当前最低维度开展学习筑基、情景测试和治理研修联动提升。");
            suggestions = Collections.singletonList(fallback);
        }

        aiAdvice.setSuggestions(suggestions);
        return aiAdvice;
    }

    private List<AssessmentHomeDTO.ComparisonInsight> buildComparisonInsights(List<AssessmentHomeDTO.DimensionCompare> compares) {
        List<AssessmentHomeDTO.ComparisonInsight> list = new ArrayList<>();
        int id = 1;

        if (compares == null || compares.isEmpty()) {
            AssessmentHomeDTO.ComparisonInsight item = new AssessmentHomeDTO.ComparisonInsight();
            item.setId(id);
            item.setType("strength");
            item.setTitle("当前暂无足够对比数据");
            item.setDetail("建议先生成更多评估记录，以便后续展示个人与群体对比分析结果。");
            list.add(item);
            return list;
        }

        List<AssessmentHomeDTO.DimensionCompare> strengths = compares.stream()
                .filter(x -> x.getPersonalScore() - x.getCompareScore() >= 5)
                .sorted(Comparator.comparingInt(x -> -(x.getPersonalScore() - x.getCompareScore())))
                .toList();

        List<AssessmentHomeDTO.DimensionCompare> weaknesses = compares.stream()
                .filter(x -> x.getCompareScore() - x.getPersonalScore() >= 5)
                .sorted(Comparator.comparingInt(x -> -(x.getCompareScore() - x.getPersonalScore())))
                .toList();

        if (!strengths.isEmpty()) {
            AssessmentHomeDTO.DimensionCompare s = strengths.get(0);
            AssessmentHomeDTO.ComparisonInsight item = new AssessmentHomeDTO.ComparisonInsight();
            item.setId(id++);
            item.setType("strength");
            item.setTitle(s.getName() + "高于对比对象平均");
            item.setDetail("该维度领先约" + (s.getPersonalScore() - s.getCompareScore()) + "分，可继续保持并发挥示范带动作用。");
            list.add(item);
        }

        if (!weaknesses.isEmpty()) {
            AssessmentHomeDTO.DimensionCompare w = weaknesses.get(0);
            AssessmentHomeDTO.ComparisonInsight item = new AssessmentHomeDTO.ComparisonInsight();
            item.setId(id++);
            item.setType("weakness");
            item.setTitle(w.getName() + "低于对比对象平均");
            item.setDetail("该维度落后约" + (w.getCompareScore() - w.getPersonalScore()) + "分，建议优先纳入下一阶段提升计划。");
            list.add(item);
        }

        AssessmentHomeDTO.DimensionCompare best = compares.stream()
                .max(Comparator.comparingInt(AssessmentHomeDTO.DimensionCompare::getPersonalScore))
                .orElse(null);
        if (best != null) {
            AssessmentHomeDTO.ComparisonInsight item = new AssessmentHomeDTO.ComparisonInsight();
            item.setId(id);
            item.setType("strength");
            item.setTitle(best.getName() + "为当前优势维度");
            item.setDetail("个人得分达到" + best.getPersonalScore() + "分，可作为阶段性重点优势继续巩固。");
            list.add(item);
        }

        return list;
    }

    private Map<String, List<AssessmentHomeDTO.TrendPoint>> buildTrendData(Integer userId, String trendRange, LocalDate today) {
        LocalDate startDate = resolveTrendStartDate(trendRange, today);
        List<Assessment> list = assessmentMapper.listByUserIdAndRange(userId, startDate, today);
        if (list.isEmpty()) {
            Assessment latest = assessmentMapper.selectLatestByUserId(userId);
            if (latest != null) {
                list = Collections.singletonList(latest);
            }
        }

        Map<String, List<AssessmentHomeDTO.TrendPoint>> out = new LinkedHashMap<>();
        out.put("learning", buildTrendPoints(list, "learning"));
        out.put("ability", buildTrendPoints(list, "ability"));
        out.put("governance", buildTrendPoints(list, "governance"));
        return out;
    }

    private List<AssessmentHomeDTO.TrendPoint> buildTrendPoints(List<Assessment> list, String module) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("M月");
        List<AssessmentHomeDTO.TrendPoint> points = new ArrayList<>();
        for (Assessment a : list) {
            AssessmentHomeDTO.TrendPoint p = new AssessmentHomeDTO.TrendPoint();
            p.setLabel(a.getRecordDate().format(formatter));
            if ("learning".equals(module)) {
                p.setScore(nullSafe(a.getLearningScore()));
            } else if ("ability".equals(module)) {
                p.setScore(nullSafe(a.getAbilityScore()));
            } else {
                p.setScore(nullSafe(a.getGovernanceScore()));
            }
            points.add(p);
        }
        return points;
    }

    private Integer calcLearningScore(Integer userId, LocalDate startDate, LocalDate endDate) {
        int completionRate = calcLearningCompletionRate(userId, startDate, endDate);
        int studySeconds = nullSafe(analyticsMapper.sumLearningStudySeconds(userId, startDate, endDate));
        int activeDays = nullSafe(analyticsMapper.countLearningActiveDays(userId, startDate, endDate));

        int studyHoursTenths = (int) Math.round(studySeconds / 360.0);
        int hourScore = percentOfTarget(studyHoursTenths, 80);
        int activeDayScore = percentOfTarget(activeDays, 12);

        return clamp((int) Math.round(completionRate * 0.5 + hourScore * 0.3 + activeDayScore * 0.2));
    }

    private Integer calcAbilityScore(Integer userId, LocalDate startDate, LocalDate endDate) {
        return clamp(nullSafe(analyticsMapper.avgAbilityScore(userId, startDate, endDate)));
    }

    private Integer calcGovernanceScore(Integer userId, LocalDate startDate, LocalDate endDate) {
        return clamp(nullSafe(analyticsMapper.avgGovernanceScore(userId, startDate, endDate)));
    }

    private Integer calcOverallScore(Integer learningScore, Integer abilityScore, Integer governanceScore) {
        return clamp((int) Math.round(learningScore * 0.3 + abilityScore * 0.35 + governanceScore * 0.35));
    }

    private Integer calcLevel(Integer score) {
        if (score >= 90) return 0;
        if (score >= 80) return 1;
        if (score >= 70) return 2;
        return 3;
    }

    private String levelText(Integer level) {
        if (level == null) return "未知";
        return switch (level) {
            case 0 -> "优秀";
            case 1 -> "良好";
            case 2 -> "合格";
            default -> "需改进";
        };
    }

    private String priorityText(Integer priority) {
        if (priority == null) return "中";
        return switch (priority) {
            case 1 -> "高";
            case 2 -> "中";
            default -> "低";
        };
    }

    private AssessmentHomeDTO.Metric metric(String name, String value, Integer percentage, String target, Integer trend) {
        AssessmentHomeDTO.Metric m = new AssessmentHomeDTO.Metric();
        m.setName(name);
        m.setValue(value);
        m.setPercentage(clamp(nullSafe(percentage)));
        m.setTarget(target);
        m.setTrend(trend);
        return m;
    }

    private Integer estimateTrend(int current, int target) {
        if (target <= 0) return 0;
        return (int) Math.round((current - target) * 100.0 / target);
    }

    private String normalizeComparisonTarget(String target) {
        if (target == null || target.isBlank()) return "department";
        return target;
    }

    private LocalDate resolveHomeStartDate(String timeRange, LocalDate today) {
        if (timeRange == null || timeRange.isBlank()) timeRange = "quarter";
        return switch (timeRange) {
            case "year" -> today.withDayOfYear(1);
            case "semester" -> today.getMonthValue() <= 6
                    ? LocalDate.of(today.getYear(), Month.FEBRUARY, 1)
                    : LocalDate.of(today.getYear(), Month.SEPTEMBER, 1);
            default -> today.minusMonths(3);
        };
    }

    private LocalDate resolveTrendStartDate(String trendRange, LocalDate today) {
        if (trendRange == null || trendRange.isBlank()) trendRange = "3m";
        return switch (trendRange) {
            case "1y" -> today.minusYears(1);
            case "6m" -> today.minusMonths(6);
            default -> today.minusMonths(3);
        };
    }

    private Integer currentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) {
            throw new SecurityException("未登录");
        }
        return userId;
    }

    private int calcLearningCompletionRate(Integer userId, LocalDate startDate, LocalDate endDate) {
        int completed = nullSafe(analyticsMapper.countCompletedLearningCourses(userId, startDate, endDate));
        int participated = nullSafe(analyticsMapper.countParticipatedLearningCourses(userId, startDate, endDate));
        return participated <= 0 ? 0 : (int) Math.round(completed * 100.0 / participated);
    }

    private int percentOfTarget(int current, int target) {
        if (target <= 0) {
            return 0;
        }
        return clamp((int) Math.round(current * 100.0 / target));
    }

    private String formatHours(Integer seconds) {
        double hours = seconds == null ? 0D : seconds / 3600.0;
        return String.format(Locale.CHINA, "%.1f", hours);
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private int clamp(Integer value) {
        if (value == null) return 0;
        return Math.max(0, Math.min(100, value));
    }

    private List<List<String>> buildHead(String... titles) {
        List<List<String>> head = new ArrayList<>();
        for (String title : titles) {
            head.add(Collections.singletonList(title));
        }
        return head;
    }

    private List<List<Object>> buildSummaryRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        rows.add(row("综合评估", "综合得分", nullSafe(home.getOverallScore())));
        rows.add(row("综合评估", "综合等级", safe(home.getOverallGrade())));
        rows.add(row("综合评估", "学习筑基得分", nullSafe(home.getLearningInvestment())));
        rows.add(row("综合评估", "能力提升得分", nullSafe(home.getAbilityPerformance())));
        rows.add(row("综合评估", "治理研修得分", nullSafe(home.getGovernanceLevel())));
        rows.add(row("综合评估", "较上期变化", nullSafe(home.getMonthlyChange())));
        rows.add(row("综合评估", "总活动数", nullSafe(home.getTotalActivities())));
        rows.add(row("综合评估", "提升率", nullSafe(home.getImprovementRate()) + "%"));
        rows.add(row("综合评估", "排名百分位", nullSafe(home.getRankingPercentile()) + "%"));
        return rows;
    }

    private List<List<Object>> buildModuleRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getModules() != null) {
            for (AssessmentHomeDTO.ModulePanel module : home.getModules().values()) {
                rows.add(row(
                        safe(module.getName()),
                        safe(module.getDescription()),
                        nullSafe(module.getScore())
                ));
            }
        }
        return rows;
    }

    private List<List<Object>> buildMetricRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getModules() != null) {
            for (AssessmentHomeDTO.ModulePanel module : home.getModules().values()) {
                if (module.getMetrics() == null) continue;
                for (AssessmentHomeDTO.Metric metric : module.getMetrics()) {
                    rows.add(row(
                            safe(module.getName()),
                            safe(metric.getName()),
                            safe(metric.getValue()),
                            nullSafe(metric.getPercentage()),
                            safe(metric.getTarget()),
                            nullSafe(metric.getTrend())
                    ));
                }
            }
        }
        return rows;
    }

    private List<List<Object>> buildDimensionRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getDimensions() != null) {
            for (AssessmentHomeDTO.DimensionScore dim : home.getDimensions()) {
                rows.add(row(
                        safe(dim.getName()),
                        nullSafe(dim.getScore())
                ));
            }
        }
        return rows;
    }

    private List<List<Object>> buildComparisonRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getComparisonDimensions() != null) {
            for (AssessmentHomeDTO.DimensionCompare dim : home.getComparisonDimensions()) {
                rows.add(row(
                        safe(dim.getName()),
                        nullSafe(dim.getPersonalScore()),
                        nullSafe(dim.getCompareScore())
                ));
            }
        }
        return rows;
    }

    private List<List<Object>> buildTrendRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getTrendData() != null) {
            for (Map.Entry<String, List<AssessmentHomeDTO.TrendPoint>> entry : home.getTrendData().entrySet()) {
                if (entry.getValue() == null) continue;
                for (AssessmentHomeDTO.TrendPoint point : entry.getValue()) {
                    rows.add(row(
                            safe(entry.getKey()),
                            safe(point.getLabel()),
                            nullSafe(point.getScore())
                    ));
                }
            }
        }
        return rows;
    }

    private List<List<Object>> buildAdviceRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getAiAdvice() != null) {
            rows.add(row("总结", "", safe(home.getAiAdvice().getSummary()), ""));
            if (home.getAiAdvice().getSuggestions() != null) {
                for (AssessmentHomeDTO.AdviceItem item : home.getAiAdvice().getSuggestions()) {
                    rows.add(row(
                            "建议",
                            safe(item.getTitle()),
                            safe(item.getDescription()),
                            safe(item.getPriority())
                    ));
                }
            }
        }
        return rows;
    }

    private List<List<Object>> buildInsightRows(AssessmentHomeDTO home) {
        List<List<Object>> rows = new ArrayList<>();
        if (home.getComparisonInsights() != null) {
            for (AssessmentHomeDTO.ComparisonInsight item : home.getComparisonInsights()) {
                rows.add(row(
                        safe(item.getTitle()),
                        safe(item.getDetail())
                ));
            }
        }
        return rows;
    }

    private List<Object> row(Object... values) {
        List<Object> row = new ArrayList<>();
        Collections.addAll(row, values);
        return row;
    }

    private String safe(String value) {
        return value == null ? "" : value;
    }
}