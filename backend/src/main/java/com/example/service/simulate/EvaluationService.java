package com.example.service.simulate;

import com.example.dto.simulate.EvaluationDetailDTO;
import com.example.dto.simulate.EvaluationHomeDTO;
import com.example.mapper.simulate.chat.ChatMessageMapper;
import com.example.mapper.simulate.chat.ChatSessionMapper;
import com.example.mapper.simulate.evaluation.*;
import com.example.mapper.test.SceneMapper;
import com.example.pojo.simulate.chat.ChatMessage;
import com.example.pojo.simulate.chat.ChatSession;
import com.example.pojo.simulate.evaluation.*;
import com.example.service.test.AIApiService;
import com.example.utils.JsonUtil;
import com.example.utils.ThreadLocalUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class EvaluationService {
    @Autowired
    private EvaluationMapper evaluationMapper;
    @Autowired
    private EvaluationItemMapper itemMapper;
    @Autowired
    private EvaluationEvidenceMapper evidenceMapper;
    @Autowired
    private EvaluationSuggestionMapper suggestionMapper;
    @Autowired
    private EvaluationDimensionMapper dimensionMapper;
    @Autowired
    private EvaluationDimensionScoreMapper dimScoreMapper;
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private SceneMapper sceneMapper;
    @Autowired
    private AIApiService AIApi;

    private static final String[] DIMENSION_COLORS = new String[]{"#409eff", "#67c23a", "#e6a23c", "#f56c6c", "#909399", "#36c"};

    private void checkOwner(Integer ownerUserId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer uid = (Integer) map.get("id");
        if (uid == null || !Objects.equals(uid, ownerUserId)) {
            throw new SecurityException("无权限访问该评估");
        }
    }

    @Transactional
    public Integer ensureEvaluation(Integer sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) throw new IllegalArgumentException("会话不存在");
        checkOwner(session.getUserId());
        if (!Objects.equals(session.getStep(), session.getMaxSteps())) throw new IllegalStateException("当前会话尚未完成全部推演轮次，不能生成评估");

        List<ChatMessage> messages = messageMapper.listBySessionId(sessionId);
        if (messages == null || messages.isEmpty()) throw new IllegalArgumentException("当前会话暂无消息，无法生成评估");

        List<EvaluationDimension> dimensions = dimensionMapper.listAll();
        if (dimensions == null || dimensions.isEmpty()) throw new IllegalStateException("evaluation_dimensions 未初始化");

        Evaluation old = evaluationMapper.selectBySessionId(sessionId);
        if (old != null) {
            evidenceMapper.deleteByEvaluationId(old.getId());
            itemMapper.deleteByEvaluationId(old.getId());
            suggestionMapper.deleteByEvaluationId(old.getId());
            dimScoreMapper.deleteByEvaluationId(old.getId());
            evaluationMapper.deleteById(old.getId());
        }

        Evaluation e = new Evaluation();
        e.setSessionId(sessionId);
        e.setUserId(session.getUserId());
        e.setStatus(0);
        evaluationMapper.insert(e);

        String system = buildSystemPrompt();
        String user = buildUserPrompt(session, messages, dimensions);
        String rawJson = AIApi.callJson(system, user, 0.4f, 1);
        log.info("LLM evaluation json: {}", rawJson);

        Map<String, Object> obj = JsonUtil.parseMap(rawJson);
        String summary = JsonUtil.asText(obj.get("summary"));
        Integer style = JsonUtil.toInt(obj.get("style"), 0);
        Integer riskLevel = JsonUtil.toInt(obj.get("riskLevel"), 2);

        List<Map<String, Object>> mainDimScores = JsonUtil.asListMap(obj.get("mainDimScores"));
        List<Map<String, Object>> processDimScores = JsonUtil.asListMap(obj.get("processDimScores"));
        List<Map<String, Object>> strengths = JsonUtil.asListMap(obj.get("strengths"));
        List<Map<String, Object>> risks = JsonUtil.asListMap(obj.get("risks"));
        List<Map<String, Object>> criticalMoments = JsonUtil.asListMap(obj.get("criticalMoments"));
        List<Map<String, Object>> suggestions = JsonUtil.asListMap(obj.get("suggestions"));

        if (mainDimScores.isEmpty() || processDimScores.isEmpty()) throw new IllegalStateException("AI未返回维度评分");
        if (strengths.size() < 2 || risks.size() < 2 || criticalMoments.isEmpty() || suggestions.size() < 3) throw new IllegalStateException("LLM输出条目数量不符合约束");

        Set<Integer> mDimIds = dimensions.stream().filter(d -> d.getType() == 0).map(EvaluationDimension::getId).collect(Collectors.toSet());
        Set<Integer> pDimIds = dimensions.stream().filter(d -> d.getType() == 1).map(EvaluationDimension::getId).collect(Collectors.toSet());
        Set<Integer> msgIds = messages.stream().map(ChatMessage::getId).filter(Objects::nonNull).collect(Collectors.toSet());

        Integer overallScore = saveMainDimScores(e.getId(), mainDimScores, mDimIds);
        saveProcessDimScores(e.getId(), processDimScores, pDimIds);
        saveItemList(e.getId(), 0, strengths, mDimIds, msgIds);
        saveItemList(e.getId(), 1, risks, mDimIds, msgIds);
        saveItemList(e.getId(), 2, criticalMoments, mDimIds, msgIds);
        saveSuggestions(e.getId(), suggestions, mDimIds);

        e.setSummary(summary);
        e.setStyle(style);
        e.setRiskLevel(riskLevel);
        e.setOverallScore(overallScore);
        e.setOverallLevel(toLevel(overallScore));
        e.setStatus(1);
        evaluationMapper.updateFull(e);

        sessionMapper.updateComplete(sessionId, "completed", "completed");
        return e.getId();
    }

    public EvaluationDetailDTO detail(Integer sessionId) {
        Evaluation e = evaluationMapper.selectBySessionId(sessionId);
        if (e == null) throw new IllegalArgumentException("当前会话尚未生成评估");
        checkOwner(e.getUserId());
        return buildDetailDTO(e);
    }

    public List<EvaluationDimension> getDimensions() {
        return dimensionMapper.listAll();
    }

    public List<Evaluation> getRecent() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer uid = (Integer) map.get("id");
        if (uid == null) {
            throw new SecurityException("未登录");
        }
        return evaluationMapper.selectRecentByUserId(uid);
    }

    public EvaluationHomeDTO getHome() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer uid = (Integer) map.get("id");
        if (uid == null) {
            throw new SecurityException("未登录");
        }

        List<Map<String, Object>> rows = evaluationMapper.selectHomeRowsByUserId(uid);
        EvaluationHomeDTO home = new EvaluationHomeDTO();
        if (rows == null || rows.isEmpty()) {
            return home;
        }

        List<EvaluationDimension> dimensions = dimensionMapper.listAll();
        List<Integer> scores = rows.stream().map(r -> toInt(r.get("overallScore"), 0)).collect(Collectors.toList());
        int overallScore = avg(scores);
        home.setOverallScore(overallScore);
        home.setScenariosCompleted(rows.size());
        home.setCorrectDecisionsRate(overallScore);
        home.setPercentile(calcPercentile(overallScore));
        home.setRanking("前" + home.getPercentile() + "%");
        home.setImprovement(calcImprovement(scores));
        home.setLearningDays(calcLearningDays(rows));
        home.getDecisionPattern().setPrimaryStyle(getStyleText(toInt(rows.get(0).get("style"), 1)));

        Map<Integer, List<Integer>> dimScoreMap = new HashMap<>();
        for (Map<String, Object> row : rows) {
            Integer evaluationId = toInt(row.get("id"), null);
            if (evaluationId == null) continue;
            List<EvaluationDimensionScore> dsList = dimScoreMapper.listByEvaluationId(evaluationId);
            for (EvaluationDimensionScore ds : dsList) {
                dimScoreMap.computeIfAbsent(ds.getDimensionId(), k -> new ArrayList<>()).add(ds.getScore());
            }
        }

        List<EvaluationHomeDTO.DimensionItem> dimItems = new ArrayList<>();
        int colorIndex = 0;
        for (EvaluationDimension dim : dimensions) {
            if (dim.getType() != 0) continue;
            EvaluationHomeDTO.DimensionItem item = new EvaluationHomeDTO.DimensionItem();
            item.setName(dim.getName());
            item.setScore(avg(dimScoreMap.getOrDefault(dim.getId(), Collections.emptyList())));
            item.setColor(DIMENSION_COLORS[colorIndex % DIMENSION_COLORS.length]);
            colorIndex++;
            dimItems.add(item);
        }
        dimItems.sort((a, b) -> Integer.compare(b.getScore(), a.getScore()));
        if (dimItems.size() > 5) {
            dimItems = new ArrayList<>(dimItems.subList(0, 5));
        }
        home.setDimensions(dimItems);
        home.setDecisionSpeed(selectMetric(dimItems, overallScore, "速度", "响应", "时效", "危机"));
        home.setDecisionQuality(selectMetric(dimItems, overallScore, "方案", "执行", "质量", "效果"));
        home.setMoralJudgment(selectMetric(dimItems, overallScore, "道德", "伦理", "师德", "规范", "价值"));
        home.getDecisionPattern().setConsistency(calcConsistency(rows));
        home.getDecisionPattern().setAvgTime(calcAvgDecisionTime(rows));
        home.setScenarioPerformances(buildScenarioPerformances(rows));

        Integer latestEvaluationId = toInt(rows.get(0).get("id"), null);
        Evaluation latest = latestEvaluationId == null ? null : evaluationMapper.selectById(latestEvaluationId);
        if (latest != null) {
            EvaluationDetailDTO detail = buildDetailDTO(latest);
            home.setStrengths(buildStrengths(detail));
            home.setImprovements(buildImprovements(detail));
            home.setCriticalMoments(buildCriticalMoments(detail, rows));
            home.setFocusAreas(buildFocusAreas(dimItems));
        }

        return home;
    }

    private EvaluationDetailDTO buildDetailDTO(Evaluation e) {
        ChatSession session = sessionMapper.selectById(e.getSessionId());

        EvaluationDetailDTO dto = new EvaluationDetailDTO();
        dto.setEvaluationId(e.getId());
        dto.setSessionId(e.getSessionId());
        dto.setSceneCategoryId(session == null ? null : session.getSceneCategoryId());
        dto.setOverallScore(e.getOverallScore());
        dto.setOverallLevel(e.getOverallLevel());
        dto.setSummary(e.getSummary());
        dto.setStyle(e.getStyle());
        dto.setRiskLevel(e.getRiskLevel());

        List<EvaluationDimensionScore> dss = dimScoreMapper.listByEvaluationId(e.getId());
        List<EvaluationDetailDTO.DimScore> dsDto = dss.stream().map(s -> {
            EvaluationDetailDTO.DimScore x = new EvaluationDetailDTO.DimScore();
            x.setId(s.getDimensionId());
            x.setScore(s.getScore());
            x.setWeight(s.getWeight());
            x.setComment(s.getComment());
            return x;
        }).collect(Collectors.toList());
        dto.setDimScores(dsDto);

        dto.setStrengths(loadItems(e.getId(), 0));
        dto.setRisks(loadItems(e.getId(), 1));
        dto.setCriticalMoments(loadItems(e.getId(), 2));

        List<EvaluationSuggestion> sug = suggestionMapper.listByEvaluationId(e.getId());
        List<EvaluationDetailDTO.Suggestion> ss = sug.stream().map(s -> {
            EvaluationDetailDTO.Suggestion x = new EvaluationDetailDTO.Suggestion();
            x.setId(s.getId());
            x.setDimensionId(s.getDimensionId());
            x.setTitle(s.getTitle());
            x.setContent(s.getContent());
            x.setPriority(s.getPriority());
            return x;
        }).collect(Collectors.toList());
        dto.setSuggestions(ss);
        return dto;
    }

    private List<EvaluationHomeDTO.ScenarioPerformanceItem> buildScenarioPerformances(List<Map<String, Object>> rows) {
        List<EvaluationHomeDTO.ScenarioPerformanceItem> list = new ArrayList<>();
        for (Map<String, Object> row : rows) {
            Integer evaluationId = toInt(row.get("id"), null);
            Integer sessionId = toInt(row.get("sessionId"), null);
            if (evaluationId == null || sessionId == null) continue;

            EvaluationHomeDTO.ScenarioPerformanceItem item = new EvaluationHomeDTO.ScenarioPerformanceItem();
            item.setEvaluationId(evaluationId);
            item.setSessionId(sessionId);
            item.setScore(toInt(row.get("overallScore"), 0));
            item.setCompletedAt(stringifyDate(row.get("updatedAt")));

            ChatSession session = sessionMapper.selectById(sessionId);
            item.setSessionTitle(buildTrainingTitle(session));
            item.setDecisionTime(session != null && session.getMaxSteps() != null ? session.getMaxSteps() * 15 : 0);

            Evaluation e = evaluationMapper.selectById(evaluationId);
            if (e != null) {
                EvaluationDetailDTO detail = buildDetailDTO(e);
                List<String> keywords = new ArrayList<>();
                if (detail.getStrengths() != null) {
                    detail.getStrengths().stream().limit(2).forEach(s -> keywords.add(shortText(s.getContent(), 10)));
                }
                if (detail.getSuggestions() != null) {
                    detail.getSuggestions().stream().limit(1).forEach(s -> keywords.add(shortText(s.getTitle(), 10)));
                }
                if (keywords.isEmpty() && e.getSummary() != null) {
                    keywords.add(shortText(e.getSummary(), 10));
                }
                item.setKeyDecisions(keywords);
            }
            list.add(item);
        }
        return list;
    }

    private String buildTrainingTitle(ChatSession session) {
        if (session == null) return "未命名训练";
        String title = safeText(session.getTitle());
        if (title != null && !title.isEmpty()) return title;
        return buildScenarioName(session);
    }

    private List<EvaluationHomeDTO.StrengthItem> buildStrengths(EvaluationDetailDTO detail) {
        List<EvaluationHomeDTO.StrengthItem> list = new ArrayList<>();
        if (detail == null || detail.getStrengths() == null) return list;
        int idx = 1;
        for (EvaluationDetailDTO.ItemWithEvidences item : detail.getStrengths().stream().limit(3).collect(Collectors.toList())) {
            EvaluationHomeDTO.StrengthItem x = new EvaluationHomeDTO.StrengthItem();
            x.setId(item.getId());
            x.setTitle(shortText(item.getContent(), 12));
            x.setDescription(item.getContent());
            x.setEvidence(item.getEvidences() != null && !item.getEvidences().isEmpty() ? item.getEvidences().get(0).getReason() : "来自本次推演评估结果");
            if (x.getTitle() == null || x.getTitle().isEmpty()) x.setTitle("决策优势" + idx);
            idx++;
            list.add(x);
        }
        return list;
    }

    private List<EvaluationHomeDTO.ImprovementItem> buildImprovements(EvaluationDetailDTO detail) {
        List<EvaluationHomeDTO.ImprovementItem> list = new ArrayList<>();
        if (detail == null || detail.getSuggestions() == null) return list;
        for (EvaluationDetailDTO.Suggestion s : detail.getSuggestions().stream().sorted(Comparator.comparing(EvaluationDetailDTO.Suggestion::getPriority, Comparator.nullsLast(Integer::compareTo))).limit(3).collect(Collectors.toList())) {
            EvaluationHomeDTO.ImprovementItem item = new EvaluationHomeDTO.ImprovementItem();
            item.setId(s.getId());
            item.setTitle(shortText(s.getTitle(), 14));
            item.setDescription(s.getContent());
            item.setPriority(toPriorityText(s.getPriority()));
            list.add(item);
        }
        return list;
    }

    private List<EvaluationHomeDTO.CriticalMomentItem> buildCriticalMoments(EvaluationDetailDTO detail, List<Map<String, Object>> rows) {
        List<EvaluationHomeDTO.CriticalMomentItem> list = new ArrayList<>();
        if (detail == null || detail.getCriticalMoments() == null) return list;

        String scenarioName = "情景训练";
        if (rows != null && !rows.isEmpty()) {
            Integer sessionId = toInt(rows.get(0).get("sessionId"), null);
            ChatSession session = sessionId == null ? null : sessionMapper.selectById(sessionId);
            scenarioName = buildScenarioName(session);
        }

        for (EvaluationDetailDTO.ItemWithEvidences cm : detail.getCriticalMoments().stream().limit(3).collect(Collectors.toList())) {
            EvaluationHomeDTO.CriticalMomentItem item = new EvaluationHomeDTO.CriticalMomentItem();
            item.setId(cm.getId());
            item.setScenario(scenarioName);
            item.setTimestamp(LocalDateTime.now().toString());
            item.setType(inferMomentType(cm.getContent()));
            item.setImpact(cm.getLevel() != null && cm.getLevel() == 1 ? "negative" : cm.getLevel() != null && cm.getLevel() == 2 ? "neutral" : "positive");
            item.setDescription(cm.getContent());
            item.setYourDecision(shortText(cm.getContent(), 18));
            item.setRecommendedDecision(detail.getSuggestions() != null && !detail.getSuggestions().isEmpty() ? shortText(detail.getSuggestions().get(0).getTitle(), 18) : "结合建议进一步优化处置策略");
            item.setLesson(cm.getEvidences() != null && !cm.getEvidences().isEmpty() ? cm.getEvidences().get(0).getReason() : "从关键节点中形成复盘意识");
            list.add(item);
        }
        return list;
    }

    private List<String> buildFocusAreas(List<EvaluationHomeDTO.DimensionItem> dimItems) {
        return dimItems.stream().sorted(Comparator.comparing(EvaluationHomeDTO.DimensionItem::getScore)).limit(3).map(EvaluationHomeDTO.DimensionItem::getName).collect(Collectors.toList());
    }

    private Integer saveMainDimScores(Integer evaluationId, List<Map<String, Object>> list, Set<Integer> dimIds) {
        BigDecimal totalWeight = BigDecimal.ZERO;
        BigDecimal weightedSum = BigDecimal.ZERO;
        for (Map<String, Object> one : list) {
            Integer dimId = JsonUtil.toInt(one.get("dimensionId"), null);
            if (dimId == null || !dimIds.contains(dimId)) throw new IllegalStateException("AI返回了非法 dimensionId: " + dimId);
            Integer score = JsonUtil.toInt(one.get("score"), null);
            if (score == null) throw new IllegalStateException("维度分数不能为空");
            score = Math.max(0, Math.min(100, score));
            BigDecimal weight = JsonUtil.parseWeight(one.get("weight"));
            if (weight == null || weight.compareTo(BigDecimal.ZERO) <= 0) weight = BigDecimal.ONE;
            totalWeight = totalWeight.add(weight);
            weightedSum = weightedSum.add(weight.multiply(BigDecimal.valueOf(score)));
            EvaluationDimensionScore ds = new EvaluationDimensionScore();
            ds.setEvaluationId(evaluationId);
            ds.setDimensionId(dimId);
            ds.setScore(score);
            ds.setWeight(weight);
            ds.setComment(JsonUtil.asText(one.get("comment")));
            dimScoreMapper.insert(ds);
        }
        if (totalWeight.compareTo(new BigDecimal("0.99")) < 0 || totalWeight.compareTo(new BigDecimal("1.01")) > 0) throw new IllegalStateException("主维度权重总和应为1，实际为 " + totalWeight);
        return weightedSum.divide(totalWeight, 0, RoundingMode.HALF_UP).intValue();
    }

    private void saveProcessDimScores(Integer evaluationId, List<Map<String, Object>> list, Set<Integer> dimIds) {
        for (Map<String, Object> one : list) {
            Integer dimId = JsonUtil.toInt(one.get("dimensionId"), null);
            if (dimId == null || !dimIds.contains(dimId)) throw new IllegalStateException("AI返回了非法 dimensionId: " + dimId);
            Integer score = JsonUtil.toInt(one.get("score"), null);
            if (score == null) throw new IllegalStateException("维度分数不能为空");
            score = Math.max(0, Math.min(100, score));
            EvaluationDimensionScore ds = new EvaluationDimensionScore();
            ds.setEvaluationId(evaluationId);
            ds.setDimensionId(dimId);
            ds.setScore(score);
            ds.setComment(JsonUtil.asText(one.get("comment")));
            dimScoreMapper.insert(ds);
        }
    }

    private void saveItemList(Integer evaluationId, int type, List<Map<String, Object>> list, Set<Integer> dimIds, Set<Integer> msgIds) {
        for (Map<String, Object> one : list) {
            Integer dimId = JsonUtil.toInt(one.get("dimensionId"), null);
            if (dimId != null && !dimIds.contains(dimId)) dimId = null;
            EvaluationItem item = new EvaluationItem();
            item.setEvaluationId(evaluationId);
            item.setType(type);
            item.setDimensionId(dimId);
            item.setContent(JsonUtil.asText(one.get("content")));
            item.setRoundNo(JsonUtil.toInt(one.get("roundNo"), null));
            item.setLevel(JsonUtil.toInt(one.get("level"), 3));
            itemMapper.insert(item);
            List<Map<String, Object>> evs = JsonUtil.asListMap(one.get("evidences"));
            for (Map<String, Object> ev : evs) {
                Integer messageId = JsonUtil.toInt(ev.get("messageId"), null);
                if (messageId != null && !msgIds.contains(messageId)) continue;
                EvaluationEvidence evidence = new EvaluationEvidence();
                evidence.setItemId(item.getId());
                evidence.setEvaluationId(evaluationId);
                evidence.setMessageId(messageId);
                evidence.setReason(JsonUtil.asText(ev.get("reason")));
                evidenceMapper.insert(evidence);
            }
        }
    }

    private void saveSuggestions(Integer evaluationId, List<Map<String, Object>> list, Set<Integer> dimIds) {
        for (Map<String, Object> one : list) {
            Integer dimId = JsonUtil.toInt(one.get("dimensionId"), null);
            if (dimId != null && !dimIds.contains(dimId)) throw new IllegalStateException("建议中出现非法 dimensionId: " + dimId);
            EvaluationSuggestion s = new EvaluationSuggestion();
            s.setEvaluationId(evaluationId);
            s.setDimensionId(dimId);
            s.setType(JsonUtil.toInt(one.get("type"), 0));
            s.setTitle(JsonUtil.asText(one.get("title")));
            s.setContent(JsonUtil.asText(one.get("content")));
            s.setPriority(JsonUtil.toInt(one.get("priority"), 3));
            suggestionMapper.insert(s);
        }
    }

    private List<EvaluationDetailDTO.ItemWithEvidences> loadItems(Integer evaluationId, int type) {
        List<EvaluationItem> list = itemMapper.listByEvaluationIdAndType(evaluationId, type);
        List<EvaluationDetailDTO.ItemWithEvidences> out = new ArrayList<>();
        for (EvaluationItem item : list) {
            EvaluationDetailDTO.ItemWithEvidences x = new EvaluationDetailDTO.ItemWithEvidences();
            x.setId(item.getId());
            x.setDimensionId(item.getDimensionId());
            x.setContent(item.getContent());
            x.setLevel(item.getLevel());
            List<EvaluationEvidence> evs = evidenceMapper.listByItemId(item.getId());
            x.setEvidences(evs.stream().map(ev -> {
                EvaluationDetailDTO.Evidence e = new EvaluationDetailDTO.Evidence();
                e.setMessageId(ev.getMessageId());
                e.setReason(ev.getReason());
                return e;
            }).collect(Collectors.toList()));
            out.add(x);
        }
        return out;
    }

    private Integer toLevel(Integer score) {
        if (score == null) return null;
        if (score >= 90) return 0;
        if (score >= 80) return 1;
        if (score >= 60) return 2;
        return 3;
    }

    private int avg(List<Integer> list) {
        if (list == null || list.isEmpty()) return 0;
        return (int) Math.round(list.stream().filter(Objects::nonNull).mapToInt(Integer::intValue).average().orElse(0));
    }

    private Integer selectMetric(List<EvaluationHomeDTO.DimensionItem> dimItems, int fallback, String... keywords) {
        for (String keyword : keywords) {
            for (EvaluationHomeDTO.DimensionItem item : dimItems) {
                if (item.getName() != null && item.getName().contains(keyword)) return item.getScore();
            }
        }
        return fallback;
    }

    private Integer calcPercentile(int overallScore) {
        if (overallScore >= 90) return 90;
        if (overallScore >= 85) return 80;
        if (overallScore >= 80) return 70;
        if (overallScore >= 70) return 55;
        if (overallScore >= 60) return 40;
        return 25;
    }

    private Integer calcImprovement(List<Integer> scores) {
        if (scores == null || scores.size() < 2) return 0;
        return scores.get(0) - scores.get(1);
    }

    private Integer calcConsistency(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        Integer latestStyle = toInt(rows.get(0).get("style"), null);
        if (latestStyle == null) return 0;
        long same = rows.stream().filter(r -> Objects.equals(toInt(r.get("style"), null), latestStyle)).count();
        return (int) Math.round(same * 100.0 / rows.size());
    }

    private Integer calcAvgDecisionTime(List<Map<String, Object>> rows) {
        if (rows == null || rows.isEmpty()) return 0;
        int total = 0;
        int count = 0;
        for (Map<String, Object> row : rows) {
            Integer sessionId = toInt(row.get("sessionId"), null);
            if (sessionId == null) continue;
            ChatSession session = sessionMapper.selectById(sessionId);
            if (session != null && session.getMaxSteps() != null) {
                total += session.getMaxSteps() * 15;
                count++;
            }
        }
        return count == 0 ? 0 : Math.round(total * 1.0f / count);
    }

    private Integer calcLearningDays(List<Map<String, Object>> rows) {
        Set<LocalDate> days = new HashSet<>();
        for (Map<String, Object> row : rows) {
            LocalDateTime dt = toLocalDateTime(row.get("updatedAt"));
            if (dt != null) days.add(dt.toLocalDate());
        }
        return days.size();
    }

    private Integer toInt(Object value, Integer defaultValue) {
        if (value == null) return defaultValue;
        if (value instanceof Integer) return (Integer) value;
        if (value instanceof Long) return ((Long) value).intValue();
        if (value instanceof Short) return ((Short) value).intValue();
        if (value instanceof BigDecimal) return ((BigDecimal) value).intValue();
        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Exception e) {
            return defaultValue;
        }
    }

    private String stringifyDate(Object value) {
        if (value == null) return "";
        if (value instanceof LocalDateTime) return ((LocalDateTime) value).truncatedTo(ChronoUnit.SECONDS).toString().replace("T", " ");
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime().truncatedTo(ChronoUnit.SECONDS).toString().replace("T", " ");
        return String.valueOf(value).replace("T", " ");
    }

    private LocalDateTime toLocalDateTime(Object value) {
        if (value == null) return null;
        if (value instanceof LocalDateTime) return (LocalDateTime) value;
        if (value instanceof Timestamp) return ((Timestamp) value).toLocalDateTime();
        try {
            return LocalDateTime.parse(String.valueOf(value).replace(" ", "T"));
        } catch (Exception e) {
            return null;
        }
    }

    private String buildScenarioName(ChatSession session) {
        if (session == null) return "情景训练";
        String scenario = safeText(session.getScenario());
        if (scenario != null && !scenario.isEmpty()) return shortText(scenario, 16);
        try {
            String categoryName = sceneMapper.findCategoryNameById(session.getSceneCategoryId());
            if (categoryName != null && !categoryName.isEmpty()) return categoryName;
        } catch (Exception ignored) {
        }
        return "情景训练";
    }

    private String getStyleText(Integer style) {
        if (style == null) return "权衡型决策";
        if (style == 0) return "稳健型决策";
        if (style == 1) return "权衡型决策";
        if (style == 2) return "激进型决策";
        return "权衡型决策";
    }

    private String shortText(String text, int len) {
        if (text == null) return "";
        String x = text.replace("\n", " ").replace("\r", " ").trim();
        if (x.length() <= len) return x;
        return x.substring(0, len) + "...";
    }

    private String toPriorityText(Integer priority) {
        if (priority == null) return "medium";
        if (priority == 1) return "high";
        if (priority == 2) return "medium";
        return "low";
    }

    private String inferMomentType(String content) {
        String text = content == null ? "" : content;
        if (text.contains("舆情") || text.contains("危机")) return "crisis";
        if (text.contains("冲突") || text.contains("关系") || text.contains("沟通")) return "conflict";
        return "ethics";
    }

    private String buildSystemPrompt() {
        return """
                你是高校教师师德治理研修系统中的“决策评估助手”。
                你需要根据沙盘演练全过程，输出一份完整的结构化决策评估结果。

                【维度权重规则】
                系统包含两类维度：
                1. 主维度（共5个）：这些维度将用于计算综合评分，你需要为每个主维度分配合理的权重（权重之和应为1），并给出0-100的分数。
                2. 过程维度（共6个）：这些维度仅用于过程分析，不参与综合评分，但你也需要给出分数（0-100）。

                【输出要求】
                 1. 只输出严格的 JSON 对象，不要包含任何 markdown 或解释文字。
                 2. dimensionId 必须严格使用输入提供的真实ID。
                 3. messageId 必须来自输入给出的真实消息ID。
                 4. strengths 输出2~3条，risks 输出2~3条，criticalMoments 输出1~2条，suggestions 不少于3条。
                 5. level/priority 字段：1=高，2=中，3=低。
                 6. 建议必须具体、可执行，不能空泛。

                【字段说明】
                 - style：决策风格，取值范围： 0=稳健型，1= 衡型型，2=激进型
                 - riskLevel：整体风险等级，取值范围：1=高，2=中，3=低
                 - level：风险等级，取值范围：1=高，2=中，3=低。
                 - priority：建议优先级，取值范围：1=高，2=中，3=低。
                 - suggestions中的type：建议类型，取值范围：1=具体建议，2=总体建议。
                 - strengths、risks、criticalMoments、suggestions 中引用的 dimensionId 必须是主维度中的 ID，不能是过程维度的 ID。
                 - 当suggestions中的type=2（总体建议）时，可将 dimensionId 设为 null。

                【输出JSON结构】
                {
                  "summary":"一句话综合总结",
                  "style":0,
                  "riskLevel":2,
                  "mainDimScores":[
                    {
                      "dimensionId":1,
                      "score":85,
                      "weight":0.20,
                      "comment":"......"
                    }
                  ],
                  "processDimScores":[
                    {
                      "dimensionId":4,
                      "score":80,
                      "comment":"......"
                    }
                  ],
                  "strengths":[
                    {
                      "dimensionId":1,
                      "content":"......",
                      "roundNo":2,
                      "level":2,
                      "evidences":[
                        {"messageId":11,"reason":"......"}
                      ]
                    }
                  ],
                  "risks":[
                    {
                      "dimensionId":2,
                      "content":"......",
                      "roundNo":3,
                      "level":1,
                      "evidences":[
                        {"messageId":15,"reason":"......"}
                      ]
                    }
                  ],
                  "criticalMoments":[
                    {
                      "dimensionId":3,
                      "content":"......",
                      "roundNo":4,
                      "level":2,
                      "evidences":[
                        {"messageId":18,"reason":"......"}
                      ]
                    }
                  ],
                  "suggestions":[
                    {
                      "dimensionId":1,
                      "type":1,
                      "title":"......",
                      "content":"......",
                      "priority":1
                    }
                  ]
                }
                """;
    }

    private String buildUserPrompt(ChatSession session, List<ChatMessage> messages, List<EvaluationDimension> dimensions) {
        String mainDim = dimensions.stream().filter(d -> d.getType() == 0).map(d -> String.format("{id:%d, name:%s, description:%s}", d.getId(), d.getName(), safeText(d.getDescription()))).collect(Collectors.joining("\n"));
        String processDim = dimensions.stream().filter(d -> d.getType() == 1).map(d -> String.format("{id:%d, name:%s, description:%s}", d.getId(), d.getName(), safeText(d.getDescription()))).collect(Collectors.joining("\n"));
        String messageStr = messages.stream().map(m -> String.format("{messageId:%d, role:%s, content:%s}", m.getId(), safeText(m.getRole()), safeText(m.getContent()))).collect(Collectors.joining("\n"));
        return String.format("""
                本次沙盘演练信息：
                - 所属情景类别：%s
                - 模拟推演轮次：%s

                本次演练实际情景：
                %s

                可用维度列表（只能引用这些dimensionId）：
                主维度：%s
                过程维度：%s

                对话消息（只能引用这些messageId作为证据）：
                %s

                请基于上述信息，严格按照系统提示的要求输出完整决策评估 JSON。
                """.formatted(sceneMapper.findCategoryNameById(session.getSceneCategoryId()), String.valueOf(session.getMaxSteps()), safeText(session.getScenario()), mainDim, processDim, messageStr));
    }

    private String safeText(String s) {
        if (s == null) return "";
        String x = s.replace("\n", " ").replace("\r", " ");
        if (x.length() > 500) x = x.substring(0, 500) + "...";
        return x;
    }
}
