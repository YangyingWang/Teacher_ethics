package com.example.service.test;

import com.example.dto.test.TemplateDTO;
import com.example.mapper.test.*;
import com.example.mapper.test.quiz.*;
import com.example.mapper.test.report.ReportDimensionMapper;
import com.example.pojo.test.*;
import com.example.pojo.test.quiz.*;
import com.example.pojo.test.report.ReportDimension;
import com.example.utils.JsonUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TemplateService {
    @Autowired
    private SceneMapper sceneMapper;
    @Autowired
    private TestTemplateMapper templateMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private JudgmentQuestionMapper judgmentMapper;
    @Autowired
    private ChoiceQuestionMapper choiceMapper;
    @Autowired
    private EssayQuestionMapper essayMapper;

    @Autowired
    private ReportDimensionMapper reportDimensionMapper;
    @Autowired
    private QuestionDimensionMapper questionDimensionMapper;

    @Autowired
    private AIApiService AIApi;
    @Autowired
    private ObjectMapper om = new ObjectMapper();

    // 单机并发锁：避免同一 sceneId 被同时生成两套模板（多机仍建议加DB唯一索引）
    private final ConcurrentHashMap<Integer, Object> locks = new ConcurrentHashMap<>();

    private static class ValidationResult {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();
        int totalScore = 0;

        boolean ok() {
            return errors.isEmpty();
        }
    }

    @Transactional
    public TestTemplate ensureTemplate(Integer sceneId) {
        if (sceneId == null) throw new IllegalArgumentException("sceneId 不能为空");

        Object lock = locks.computeIfAbsent(sceneId, k -> new Object());
        synchronized (lock) {
            try {
                TestTemplate existed = templateMapper.selectBySceneId(sceneId);
                if (existed != null) return existed;

                Scene scene = sceneMapper.findSceneDetailById(sceneId);
                if (scene == null) throw new IllegalArgumentException("sceneId 不存在: " + sceneId);

                List<ReportDimension> dims = reportDimensionMapper.listAll();
                if (dims == null || dims.isEmpty())
                    throw new IllegalStateException("report_dimensions 为空，请先初始化维度");

                String system = """
                        你是高校教师职业伦理测评专家。
                        你必须输出严格的 json 对象，不要输出任何多余文字，不要markdown。
                        题目要紧扣情景，聚焦师德师风、利益冲突、学术诚信、师生边界、资源使用、权力与公平等伦理议题。
                        """;

                String user = buildGeneratePrompt(scene, dims);

                String json1 = AIApi.callJson(system, user, 0.5f, 1);
                TemplateDTO dto1 = parse(json1);
                ValidationResult v1 = validate(dto1, dims);

                if (!v1.ok()) {
                    // 一次修复重试：只修正数量/字段/多选比例，保持语义一致
                    logErrors(sceneId, v1);
                    logWarnings(sceneId, v1);
                    String fixUser = buildFixPrompt(scene, dims, json1, v1);
                    String json2 = AIApi.callJson(system, fixUser, 0.3f, 1);
                    TemplateDTO dto2 = parse(json2);
                    ValidationResult v2 = validate(dto2, dims);
                    if (!v2.ok()) {
                        throw new IllegalStateException("模板生成失败：结构校验仍不通过：" + String.join("；", v2.errors));
                    }
                    logWarnings(sceneId, v2);
                    return persistTemplate(sceneId, dto2, json2, v2.totalScore);
                }
                logWarnings(sceneId, v1);
                return persistTemplate(sceneId, dto1, json1, v1.totalScore);
            } finally {
                locks.remove(sceneId, lock);
            }
        }
    }

    private TestTemplate persistTemplate(Integer sceneId, TemplateDTO dto, String rawJson, int totalScore) {
        // 入库 questions + 子表，并记录顺序
        List<Integer> qIds = new ArrayList<>();

        for (TemplateDTO.Q q : dto.getQuestions()) {
            // 1) base question
            Question base = new Question();
            base.setSceneId(sceneId);
            base.setType(q.getType());
            base.setContent(q.getContent());
            base.setAnalysis(q.getAnalysis());
            base.setDifficulty(q.getDifficulty());
            base.setScore(q.getScore());
            questionMapper.insert(base);

            Integer qId = base.getId();
            qIds.add(qId);

            // 2) subtype
            if (q.getType() == 0) {
                JudgmentQuestion jq = new JudgmentQuestion();
                jq.setQuestionId(qId);
                jq.setCorrectAnswer(Boolean.TRUE.equals(q.getCorrectAnswer()) ? 1 : 0);
                judgmentMapper.insert(jq);
            } else if (q.getType() == 1) {
                ChoiceQuestion cq = new ChoiceQuestion();
                cq.setQuestionId(qId);
                cq.setOptionA(q.getOptionA());
                cq.setOptionB(q.getOptionB());
                cq.setOptionC(q.getOptionC());
                cq.setOptionD(q.getOptionD());
                cq.setIsMultiple(Boolean.TRUE.equals(q.getIsMultiple()) ? 1 : 0);
                cq.setCorrectAnswer(JsonUtil.toJson(q.getCorrectAnswers()));
                choiceMapper.insert(cq);
            } else if (q.getType() == 2) {
                EssayQuestion eq = new EssayQuestion();
                eq.setQuestionId(qId);
                eq.setReference(q.getReference());
                eq.setKeyword(q.getKeyword());
                essayMapper.insert(eq);
            }

            // 3) dimensions -> question_dimensions
            BigDecimal sum = BigDecimal.ZERO;
            for (TemplateDTO.QD d : q.getDimensions()) {
                QuestionDimension qd = new QuestionDimension();
                qd.setQuestionId(qId);
                qd.setDimensionId(d.getDimensionId());
                qd.setWeight(d.getWeight());
                questionDimensionMapper.insert(qd);
                sum = sum.add(d.getWeight());
            }
            // 权重总和校验（容忍误差 0.01）
            if (sum.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
                throw new IllegalStateException("题目维度权重之和不为1，questionId=" + qId + ", sum=" + sum);
            }
        }

        TestTemplate t = new TestTemplate();
        t.setSceneId(sceneId);
        t.setTitle(dto.getTitle());
        t.setQuestionSequence(JsonUtil.toJson(qIds));
        t.setTotalScore(totalScore);
        t.setTotalCount(dto.getQuestions().size());
        t.setRawJson(rawJson);
        templateMapper.insert(t);
        return t;
    }

    private void logWarnings(Integer sceneId, ValidationResult v) {
        if (v.warnings.isEmpty()) return;
        log.warn("sceneId={} 模板生成警告：{}", sceneId, String.join("；", v.warnings));
    }

    private void logErrors(Integer sceneId, ValidationResult v) {
        if (v.errors.isEmpty()) return;
        log.warn("sceneId={} 模板生成错误：{}", sceneId, String.join("；", v.errors));
    }

    private TemplateDTO parse(String json) {
        try {
            return om.readValue(json, TemplateDTO.class);
        } catch (Exception e) {
            throw new IllegalStateException("JSON 解析失败: " + e.getMessage());
        }
    }

    private ValidationResult validate(TemplateDTO dto, List<ReportDimension> dims) {
        ValidationResult vr = new ValidationResult();

        if (dto == null) {
            vr.errors.add("dto 为空");
            return vr;
        }
        if (dto.getTitle() == null || dto.getTitle().isBlank()) {
            vr.errors.add("title 为空");
            return vr;
        }
        if (dto.getQuestions() == null) {
            vr.errors.add("questions 为空");
            return vr;
        }

        List<TemplateDTO.Q> qs = dto.getQuestions();
        int total = qs.size();
        if (total < 18 || total > 28) vr.errors.add("总题数必须在 18~28 之间，当前=" + total);

        Set<Integer> validDimIds = dims.stream().map(ReportDimension::getId).collect(Collectors.toSet());
        Set<String> abcd = Set.of("A", "B", "C", "D");

        int nJ = 0, nC = 0, nE = 0;
        int nMulti = 0, nSingle = 0;
        int totalScore = 0;

        for (int i = 0; i < qs.size(); i++) {
            TemplateDTO.Q q = qs.get(i);
            String idx = "第" + (i + 1) + "题";

            if (q.getType() == null) {
                vr.errors.add(idx + " type 为空");
                continue;
            }
            if (q.getContent() == null || q.getContent().isBlank()) vr.errors.add(idx + " content 为空");
            if (q.getAnalysis() == null || q.getAnalysis().isBlank()) vr.errors.add(idx + " analysis 为空");
            if (q.getDifficulty() == null || q.getDifficulty() < 1 || q.getDifficulty() > 5)
                vr.errors.add(idx + " difficulty 必须1~5");
            if (q.getScore() == null || q.getScore() <= 0) vr.errors.add(idx + " score 必须为正整数");

            if (q.getDimensions() == null || q.getDimensions().isEmpty() || q.getDimensions().size() > 2) {
                vr.errors.add(idx + " dimensions 必须为 1~2 个");
            } else {
                BigDecimal sum = BigDecimal.ZERO;
                for (TemplateDTO.QD d : q.getDimensions()) {
                    if (d.getDimensionId() == null || !validDimIds.contains(d.getDimensionId())) {
                        vr.errors.add(idx + " dimensions.dimensionId 非法");
                        continue;
                    }
                    if (d.getWeight() == null) {
                        vr.errors.add(idx + " dimensions.weight 为空");
                        continue;
                    }
                    if (d.getWeight().compareTo(BigDecimal.ZERO) <= 0 || d.getWeight().compareTo(BigDecimal.ONE) > 0) {
                        vr.errors.add(idx + " dimensions.weight 必须在 (0,1] 内");
                        continue;
                    }
                    sum = sum.add(d.getWeight());
                }
                if (sum.subtract(BigDecimal.ONE).abs().compareTo(new BigDecimal("0.01")) > 0) {
                    vr.errors.add(idx + " dimensions.weight 之和必须=1，当前=" + sum);
                }
            }

            Integer type = q.getType();
            Integer score = q.getScore() == null ? 0 : q.getScore();
            totalScore += score;

            if (type == 0) {
                nJ++;
                if (score < 6 || score > 12) vr.errors.add(idx + " 判断题 score 必须 6~12");
                if (q.getCorrectAnswer() == null) vr.errors.add(idx + " 判断题 correctAnswer 不能为空");
            } else if (type == 1) {
                nC++;
                if (score < 3 || score > 8) vr.errors.add(idx + " 选择题 score 必须 3~8");
                if (anyBlank(q.getOptionA(), q.getOptionB(), q.getOptionC(), q.getOptionD())) {
                    vr.errors.add(idx + " 选择题 A-D 选项不能为空");
                }
                if (q.getCorrectAnswers() == null || q.getCorrectAnswers().isEmpty()) {
                    vr.errors.add(idx + " 选择题 correctAnswers 不能为空数组");
                } else {
                    // 去空、去重、校验 A/B/C/D
                    List<String> ca = q.getCorrectAnswers().stream()
                            .filter(Objects::nonNull)
                            .map(String::trim)
                            .map(String::toUpperCase)
                            .filter(s -> !s.isEmpty())
                            .distinct()
                            .toList();

                    if (ca.isEmpty()) vr.errors.add(idx + " 选择题 correctAnswers 不能为空");
                    if (!ca.stream().allMatch(abcd::contains))
                        vr.errors.add(idx + " 选择题 correctAnswers 只能包含 A/B/C/D");

                    boolean isMulti = Boolean.TRUE.equals(q.getIsMultiple());
                    if (isMulti) {
                        nMulti++;
                        if (ca.size() < 2) vr.errors.add(idx + " 多选题 correctAnswers 至少2个");
                    } else {
                        nSingle++;
                        if (ca.size() != 1) vr.errors.add(idx + " 单选题 correctAnswers 必须恰好1个");
                    }
                }
            } else if (type == 2) {
                nE++;
                if (score < 8 || score > 20) vr.errors.add(idx + " 简答题 score 必须 8~20");
                if (q.getReference() == null || q.getReference().isBlank())
                    vr.errors.add(idx + " 简答题 reference 不能为空");
                if (q.getKeyword() == null || q.getKeyword().isBlank()) vr.errors.add(idx + " 简答题 keyword 不能为空");
            } else {
                vr.errors.add(idx + " type 非法（只能 0/1/2）");
            }
        }

        vr.totalScore = totalScore;

        // 数量下限
        if (nJ < 5) vr.errors.add("判断题至少 5 题，当前=" + nJ);
        if (nC < 10) vr.errors.add("选择题至少 10 题，当前=" + nC);
        if (nE < 3) vr.errors.add("简答题至少 3 题，当前=" + nE);

        // 占比（按你给的建议区间，作为硬约束执行；如果你想改为软约束我也能改）
        if (total > 0) {
            double rJ = nJ * 1.0 / total;
            double rC = nC * 1.0 / total;
            double rE = nE * 1.0 / total;

            if (rJ < 0.20 || rJ > 0.30) vr.warnings.add(String.format("判断题占比应 20%%~30%%，当前=%.1f%%", rJ * 100));
            if (rC < 0.50 || rC > 0.70) vr.warnings.add(String.format("选择题占比应 50%%~70%%，当前=%.1f%%", rC * 100));
            if (rE < 0.10 || rE > 0.20) vr.warnings.add(String.format("简答题占比应 10%%~20%%，当前=%.1f%%", rE * 100));
        }

        // 必须含单选+多选 & 多选比例范围
        if (nC > 0) {
            if (nMulti <= 0 || nSingle <= 0)
                vr.warnings.add("选择题必须同时包含单选与多选（当前单选=" + nSingle + "，多选=" + nMulti + ")");
            double rM = nMulti * 1.0 / nC;
            if (rM < 0.30 || rM > 0.50) vr.warnings.add(String.format("多选比例应 30%%~50%%，当前=%.1f%%", rM * 100));
        }

        // 总分建议：软提醒，不作为失败条件
        if (totalScore < 120 || totalScore > 180) {
            vr.warnings.add("总分建议控制在 120~180，当前总分=" + totalScore + "（不强制）");
        }

        return vr;
    }

    private boolean anyBlank(String... s) {
        for (String x : s) if (x == null || x.isBlank()) return true;
        return false;
    }

    private String buildGeneratePrompt(Scene scene, List<ReportDimension> dims) {
        String dimText = dims.stream()
                .map(d -> d.getId() + "：" + d.getName() + "（" + d.getDescription() + "）")
                .collect(Collectors.joining("\n"));
        return """
                请基于以下情景生成一套《高校教师职业伦理情景测试》模板，输出严格 json（不要代码块，不要多余文字）。
                情景信息：
                - 情景名称：%s
                - 详细描述：%s
                - 关注点：%s
                - 事件分析：%s
                - 正确处置路径：%s
                - 错误处置路径：%s
                - 难度：%s
                                
                能力维度列表（dimensionId : name）：
                %s
                                
                必须满足的硬性规则：
                - 总题数必须在 18~28 之间
                - 题型占比建议：
                  * 判断题(type=0，correctAnswer=true/false)：约 20%%~30%%，且至少 5 题
                  * 选择题(type=1，A-D 四选；correctAnswers 为数组)：约 50%%~70%%，且至少 10 题
                  * 简答题(type=2，reference 参考答案，keyword 得分关键词)：约 10%%~20%%，且至少 3 题
                - 选择题必须含单选与多选：
                  * 多选比例目标 40%%，允许 30%%~50%%
                  * 多选题 isMultiple=true，correctAnswers 至少 2 个，如["A","C"]
                  * 单选题 isMultiple=false，correctAnswers 恰好 1 个，如 ["A"]
                - 分值规则（必须为正整数）：
                  * 判断题 score：6~12
                  * 选择题 score：3~8
                  * 简答题 score：8~20
                - 总分建议控制在 120~180（仅建议，优先保证结构与质量）
                                
                每题必须绑定 1~2 个维度（dimensions）：
                - dimensions 是数组，元素为 {"dimensionId":整数, "weight":0~1小数}
                - 同一题目下 weight 之和必须=1
                - dimensionId 只能来自上面列表
                                
                每题必须包含：type、content、analysis、difficulty(1-5)、score、dimensions。
                                
                输出 json 结构示例（必须同结构），其中questions 数组长度必须等于20!!!
                {
                  "title":"xxx",
                  "questions":[
                   {"type":0,"content":"...","analysis":"...","difficulty":3,"score":10,"correctAnswer":true,
                    "dimensions":[{"dimensionId":1,"weight":1.0}]},
                   {"type":1,"content":"...","analysis":"...","difficulty":3,"score":5,
                    "optionA":"...","optionB":"...","optionC":"...","optionD":"...",
                    "isMultiple":false,"correctAnswers":["A"],
                    "dimensions":[{"dimensionId":2,"weight":0.7},{"dimensionId":5,"weight":0.3}]},
                   {"type":2,"content":"...","analysis":"...","difficulty":3,"score":10,
                    "reference":"...","keyword":"...",
                    "dimensions":[{"dimensionId":4,"weight":1.0}]}
                 ]
                }
                """.formatted(scene.getTitle(), scene.getDescription(), scene.getFocus(), scene.getAnalysis(),
                scene.getCorrectApproach(), scene.getIncorrectApproach(), String.valueOf(scene.getDifficulty()), dimText);
    }

    private String buildFixPrompt(Scene scene, List<ReportDimension> dims, String badJson, ValidationResult vr) {
        String dimIds = dims.stream().map(d -> String.valueOf(d.getId())).collect(Collectors.joining(","));
        String errs = vr == null ? "" : String.join("；", vr.errors);
        String warns = vr == null || vr.warnings.isEmpty() ? "" : String.join("；", vr.warnings);

        return """
                你上一次输出的 json 结构不合规。请在保持题目贴合情景的前提下，严格按规则修复并输出严格 json（不要多余文字）。
                                
                不合规点（必须修复）：
                %s
                                
                软性建议（可尽量满足，不作为必须）：
                %s
                                
                必须满足的硬性规则：
                - 总题数必须在 18~28
                - 判断题(type=0)：占比 20%%~30%%，且至少 5 题，score 6~12，correctAnswer=true/false
                - 选择题(type=1)：占比 50%%~70%%，且至少 10 题，score 3~8
                  * 必须同时包含单选与多选
                  * 多选比例 30%%~50%%
                  * 多选 isMultiple=true，correctAnswers 至少 2 个
                  * 单选 isMultiple=false，correctAnswers 恰好 1 个
                  * correctAnswers 元素只能为 A/B/C/D
                - 简答题(type=2)：占比 10%%~20%%，且至少 2 题，score 8~20，必须有 reference/keyword
                - difficulty 必须是 1~5 整数
                - dimensions 1~2个；dimensionId 只能从这些ID中选：%s；weight 为 (0,1] 小数且总和=1
                                
                只输出 json 对象，不要任何解释文本。
                情景名称：%s
                                
                这是你上一次不合规的输出，请基于它修复：
                %s
                """.formatted(errs.isBlank() ? "（未提供错误明细）" : errs, warns.isBlank() ? "（无）" : warns,
                dimIds, scene.getTitle(), badJson
        );
    }
}
