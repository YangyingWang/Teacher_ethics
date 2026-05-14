package com.example.service.test;

import com.example.dto.test.ReportDetailDTO;
import com.example.dto.test.ReportQuizFactDTO;
import com.example.mapper.test.quiz.QuestionnaireMapper;
import com.example.mapper.test.report.*;
import com.example.pojo.PageBean;
import com.example.pojo.test.quiz.Questionnaire;
import com.example.pojo.test.report.*;
import com.example.utils.JsonUtil;
import com.example.utils.ThreadLocalUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReportService {
    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private ReportDimensionMapper dimMapper;
    @Autowired
    private ReportItemMapper srMapper;
    @Autowired
    private ReportEvidenceMapper evidenceMapper;
    @Autowired
    private ReportSuggestionMapper suggestionMapper;
    @Autowired
    private ReportDimensionScoreMapper dimScoreMapper;
    @Autowired
    private QuestionnaireMapper questionnaireMapper;
    @Autowired
    private ReportQuizFactMapper factMapper;
    @Autowired
    private AIApiService AIApi;

    private void checkOwner(Integer ownerUserId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer uid = (Integer) map.get("id");
        if (uid == null || !Objects.equals(uid, ownerUserId)) {
            throw new SecurityException("无权限访问该报告");
        }
    }
    @Transactional
    public void buildReport(Questionnaire qn, Map<Integer, QuizService.DimAgg> dimAgg) {
        if (qn.getStatus() == null || qn.getStatus() != 1) throw new IllegalStateException("答卷未提交，不能生成报告");

        Integer qnId = qn.getId();
        Report r = reportMapper.selectByQnId(qn.getId());
        if (r == null) {
            r = new Report();
            r.setUserId(qn.getUserId());
            r.setQuestionnaireId(qnId);
            r.setSceneId(qn.getSceneId());
            r.setStatus(0);
            r.setTotalScore(qn.getTotalScore());
            r.setUserTotalScore(qn.getUserTotalScore());
            r.setTimeSpent(qn.getTimeSpent());
            r.setCode("RPT-" + System.currentTimeMillis() + "-" + qn.getUserId());
            reportMapper.insert(r);
        } else {
            // 覆盖重算：先清空维度分（全文案在 ensureFull 会再清）
            dimScoreMapper.deleteByReportId(r.getId());
        }

        // 用 rawTotal 占比做权重（多维度映射下也能自然归一化）
        BigDecimal totalRaw = dimAgg.values().stream()
                .map(a -> a.rawTotal == null ? BigDecimal.ZERO : a.rawTotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalRaw.compareTo(BigDecimal.ZERO) <= 0) totalRaw = BigDecimal.ONE;

        BigDecimal overallWeightedSum = BigDecimal.ZERO;

        for (Map.Entry<Integer, QuizService.DimAgg> e : dimAgg.entrySet()) {
            Integer dimId = e.getKey();
            QuizService.DimAgg a = e.getValue();

            int rawTotal = a.rawTotal.setScale(0, RoundingMode.HALF_UP).intValue();
            int rawScore = a.rawScore.setScale(0, RoundingMode.HALF_UP).intValue();
            int dimScore = rawTotal == 0 ? 0 : (int) Math.round(rawScore * 100.0 / rawTotal);
            BigDecimal weight = a.rawTotal.divide(totalRaw, 6, RoundingMode.HALF_UP);

            ReportDimensionScore s = new ReportDimensionScore();
            s.setReportId(r.getId());
            s.setDimensionId(dimId);
            s.setScore(dimScore);
            s.setRawScore(rawScore);
            s.setRawTotal(rawTotal);
            s.setQuestionCount(a.questionCount);
            s.setWeight(weight.setScale(2, RoundingMode.HALF_UP));
            s.setWrongCount(a.wrongCount);
            s.setLowCount(a.lowCount);
            dimScoreMapper.insert(s);

            // overall 用“维度题量”加权平均
            overallWeightedSum = overallWeightedSum.add(weight.multiply(BigDecimal.valueOf(dimScore)));
        }
        int overallScore = overallWeightedSum.setScale(0, RoundingMode.HALF_UP).intValue();
        overallScore = Math.max(0, Math.min(100, overallScore));

        r.setOverallScore(overallScore);
        r.setOverallLevel(toLevel(overallScore));
        r.setStatus(1);
        reportMapper.updatePartial(r);
    }

    @Transactional
    public Report ensureFull(Integer qnId) {
        Report r = reportMapper.selectByQnId(qnId);
        if (r == null) throw new IllegalStateException("该答卷尚未生成PARTIAL报告，请先在提交后生成PARTIAL");
        checkOwner(r.getUserId());
        if (r.getStatus() != null && r.getStatus() == 2) return r;

        // 每次 ensureFull：先清空旧文案（支持 FAILED 复试、重提交后也干净）
        srMapper.deleteByReportId(r.getId());
        evidenceMapper.deleteByReportId(r.getId());
        suggestionMapper.deleteByReportId(r.getId());

        // 维度合法集合（校验 LLM 输出 dimensionId）
        Set<Integer> dimIds = dimMapper.listAll().stream()
                .map(ReportDimension::getId)
                .collect(Collectors.toSet());

        Questionnaire qn = questionnaireMapper.selectById(qnId);
        if (qn == null) throw new IllegalStateException("答卷不存在");

        List<ReportDimensionScore> dimScores = dimScoreMapper.listByReportId(r.getId());
        List<ReportQuizFactDTO> facts = pickFacts(factMapper.listFactsByQnId(qnId));

        // 调 LLM 生成 strengths/risks/suggestions 文案，组装输入：总体得分、维度得分、每题详情（题干/正确/我的/得分/解析...）
        String system = systemPrompt();
        String user = buildFullPrompt(r, qn, dimScores, facts);
        String rawJson = AIApi.callJson(system, user, 0.4f,1);
        log.info("LLM report json (truncated) = {}", rawJson.length() > 500 ? rawJson.substring(0, 500) + "..." : rawJson);

        // 解析 LLM 输出（简单 JSON 结构），落库 strengths/risks/suggestions/evidences
        Map<String, Object> obj = JsonUtil.parseMap(rawJson);
        String summary = JsonUtil.asText(obj.get("summary"));
        List<Map<String, Object>> strengths = JsonUtil.asListMap(obj.get("strengths"));
        List<Map<String, Object>> risks = JsonUtil.asListMap(obj.get("risks"));
        List<Map<String, Object>> suggestions = JsonUtil.asListMap(obj.get("suggestions"));
        if (strengths.size() < 2 || risks.size() < 2 || suggestions.size() != 3) {
            throw new IllegalStateException("LLM输出条目数量不符合约束");
        }

        // 允许的题目ID集合
        Set<Integer> qIds = facts.stream()
                .map(ReportQuizFactDTO::getQId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        saveSrList(r.getId(), 1, strengths, qIds);
        saveSrList(r.getId(), 2, risks, qIds);
        saveSuggestions(r.getId(), suggestions, dimIds);

        r.setStatus(2);
        r.setSummary(summary);
        r.setRawJson(rawJson);
        reportMapper.updateFull(r);

        return r;
    }

    public ReportDetailDTO detail(Integer rId) {
        Report r = reportMapper.selectById(rId);
        if (r == null) throw new IllegalStateException("未找到该报告");
        checkOwner(r.getUserId());
        if (r.getStatus() != null && r.getStatus() == 1)
            throw new IllegalStateException("该答卷尚未生成完整报告，请先生成完整报告！");

        ReportDetailDTO dto = new ReportDetailDTO();
        dto.setReportId(r.getId());
        dto.setQnId(r.getQuestionnaireId());
        dto.setSceneId(r.getSceneId());
        dto.setTotalScore(r.getTotalScore());
        dto.setUserTotalScore(r.getUserTotalScore());
        dto.setTimeSpent(r.getTimeSpent());
        dto.setOverallScore(r.getOverallScore());
        dto.setOverallLevel(r.getOverallLevel());
        dto.setSummary(r.getSummary());
        dto.setCode(r.getCode());

        // 维度分
        List<ReportDimensionScore> dss = dimScoreMapper.listByReportId(r.getId());
        List<ReportDetailDTO.DimScore> dsDto = dss.stream().map(s -> {
            ReportDetailDTO.DimScore x = new ReportDetailDTO.DimScore();
            x.setId(s.getDimensionId());
            x.setScore(s.getScore());
            x.setQuestionCount(s.getQuestionCount());
            x.setWrongCount(s.getWrongCount());
            x.setLowCount(s.getLowCount());
            return x;
        }).collect(Collectors.toList());
        dto.setDimScores(dsDto);

        // strengths/risks + evidences
        dto.setStrengths(loadItems(r.getId(), 1));
        dto.setRisks(loadItems(r.getId(), 2));

        // suggestions
        List<ReportSuggestion> sug = suggestionMapper.listByReport(r.getId());
        List<ReportDetailDTO.Suggestion> ss = sug.stream().map(s -> {
            ReportDetailDTO.Suggestion x = new ReportDetailDTO.Suggestion();
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

    public List<ReportDimension> getDimensions() {
        return dimMapper.listAll();
    }
    public List<Report> getRecent() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) throw new SecurityException("未登录");
        return reportMapper.selectRecentByUserId(userId);
    }

    public PageBean<Report> list(Integer pageNum, Integer pageSize) {
        PageBean<Report> pq = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);

        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) throw new SecurityException("未登录");

        List<Report> list = reportMapper.selectByUserId(userId);
        Page<Report> p = (Page<Report>) list;
        pq.setTotal(p.getTotal());
        pq.setItems(p.getResult());
        return pq;
    }

    private void saveSrList(Integer reportId, int type, List<Map<String, Object>> list, Set<Integer> qIds) {
        for (Map<String, Object> one : list) {
            ReportItem sr = new ReportItem();
            sr.setReportId(reportId);
            sr.setType(type);
            sr.setContent(JsonUtil.asText(one.get("content")));
            sr.setLevel(JsonUtil.toInt(one.get("level"), 3));
            srMapper.insert(sr);

            List<Map<String, Object>> evs = JsonUtil.asListMap(one.get("evidences"));
            for (Map<String, Object> ev : evs) {
                Integer qid = JsonUtil.toInt(ev.get("questionId"), null);
                if (qid == null || !qIds.contains(qid)) continue;

                ReportEvidence e = new ReportEvidence();
                e.setItemId(sr.getId());
                e.setReportId(reportId);
                e.setQuestionId(qid);
                e.setReason(JsonUtil.asText(ev.get("reason")));
                evidenceMapper.insert(e);
            }
        }
    }

    private void saveSuggestions(Integer reportId, List<Map<String, Object>> list, Set<Integer> dimIds) {
        for (Map<String, Object> one : list) {
            Integer dimId = JsonUtil.toInt(one.get("dimensionId"), null);
            if (dimId == null || !dimIds.contains(dimId)) {
                throw new IllegalStateException("建议中出现非法 dimensionId: " + dimId);
            }

            ReportSuggestion s = new ReportSuggestion();
            s.setReportId(reportId);
            s.setDimensionId(dimId);
            s.setPriority(JsonUtil.toInt(one.get("priority"), 3));
            s.setTitle(JsonUtil.asText(one.get("title")));
            s.setContent(JsonUtil.asText(one.get("content")));
            suggestionMapper.insert(s);
        }
    }

    private List<ReportDetailDTO.ItemWithEvidences> loadItems(Integer reportId, int type) {
        List<ReportItem> list = srMapper.listByReportAndType(reportId, type);
        List<ReportDetailDTO.ItemWithEvidences> out = new ArrayList<>();
        for (ReportItem sr : list) {
            ReportDetailDTO.ItemWithEvidences x = new ReportDetailDTO.ItemWithEvidences();
            x.setId(sr.getId());
            x.setContent(sr.getContent());
            x.setLevel(sr.getLevel());

            List<ReportEvidence> evs = evidenceMapper.listByItemId(sr.getId());
            x.setEvidences(evs.stream().map(ev -> {
                ReportDetailDTO.Evidence e = new ReportDetailDTO.Evidence();
                e.setQuestionId(ev.getQuestionId());
                e.setReason(ev.getReason());
                return e;
            }).collect(Collectors.toList()));

            out.add(x);
        }
        return out;
    }

    private String systemPrompt() {
        return """
                你是高校教师师德师风能力诊断助手。你只负责生成诊断文案，不要计算任何分数。
                你必须输出严格JSON（不要markdown、不要解释性文字），输出结构必须为：
                {
                  "summary":"一句话总结",
                  "strengths":[{"content":"...","level":1,"evidences":[{"questionId":1,"reason":"..."}]}],
                  "risks":[{"content":"...","level":1,"evidences":[{"questionId":2,"reason":"..."}]}],
                  "suggestions":[{"dimensionId":1,"type":5,"priority":1,"title":"...","content":"..."}]
                }

                约束：
                1) strengths 2~3条，risks 2~3条，suggestions 恰好3条；
                2) level/priority：1高 2中 3低；
                3) evidences 引用输入中真实存在的 questionId；
                4) suggestions.dimensionId 必须是输入里维度列表的id；
                5) 建议要可执行、可操作，避免空泛口号。
                6) JSON必须可解析，字段齐全。
                """;
    }

    private String buildFullPrompt(Report report, Questionnaire qn, List<ReportDimensionScore> dimScores, List<ReportQuizFactDTO> facts) {
        // 维度分字符串
        String ds = dimScores.stream()
                .map(s -> String.format("{dimensionId:%d, score:%d, rawScore:%d, rawTotal:%d, wrongCount:%d, lowCount:%d}",
                        s.getDimensionId(), s.getScore(), nvl(s.getRawScore()), nvl(s.getRawTotal()), nvl(s.getWrongCount()), nvl(s.getLowCount())))
                .collect(Collectors.joining("\n"));

        // facts（精简后的）
        String fs = facts.stream()
                .map(f -> String.format(
                    "{questionId:%s, type:%s, fullScore:%s, userScore:%s, content:%s, analysis:%s, userAnswer:%s, correctAnswers:%s, correctTF:%s, reference:%s, keyword:%s, isMultiple:%s}",
                        nvl(f.getQId()), nvl(f.getType()), nvl(f.getFullScore()), nvl(f.getUserScore()),
                        safeText(f.getContent()), safeText(f.getAnalysis()),
                        safeText(f.getUserAnswer()), safeText(f.getCorrectAnswers()),
                        f.getCorrectAnswerTf() == null ? "" : String.valueOf(f.getCorrectAnswerTf()),
                        safeText(f.getReference()), safeText(f.getKeyword()),
                        f.getIsMultiple() == null ? "false" : String.valueOf(f.getIsMultiple())
                )).collect(Collectors.joining("\n"));

        return """
                本次答卷信息：
                - qnId：%d
                - sceneId：%d
                - overallScore(0-100)：%s
                - overallLevel：%s
                            
                维度得分（0-100）与统计：
                %s
                            
                候选证据题 facts（你只能从这些 questionId 里选 evidences）：
                %s
                            
                要求：
                - strengths 2~3条，每条给出 1~2 个证据题（questionId + reason）
                - risks 2~3条，每条给出 1~2 个证据题
                - suggestions 恰好3条，必须落到具体维度（dimensionId），并给出可执行建议（含行动步骤/频率/检查点）
                """.formatted(qn.getId(), qn.getSceneId(), String.valueOf(report.getOverallScore()),
                String.valueOf(report.getOverallLevel()), ds, fs);
    }

    private List<ReportQuizFactDTO> pickFacts(List<ReportQuizFactDTO> facts) {
        if (facts == null || facts.isEmpty()) return List.of();

        List<ReportQuizFactDTO> sorted = new ArrayList<>(facts);
        // 按“丢分差距”从大到小排
        sorted.sort((a, b) -> {
            int da = nvl(a.getFullScore()) - nvl(a.getUserScore());
            int db = nvl(b.getFullScore()) - nvl(b.getUserScore());
            return Integer.compare(db, da);
        });
        // 选前 8 条作为证据候选
        return sorted.stream().limit(8).toList();
    }

    private int nvl(Integer x) {
        return x == null ? 0 : x;
    }

    private String safeText(String s) {
        if (s == null) return "";
        String x = s.replace("\n", " ").replace("\r", " ");
        if (x.length() > 200) x = x.substring(0, 200) + "...";
        return x;
    }

    private int toLevel(Integer score) {
        if (score >= 90) return 0;
        if (score >= 80) return 1;
        if (score >= 60) return 2;
        return 3;
    }
}
