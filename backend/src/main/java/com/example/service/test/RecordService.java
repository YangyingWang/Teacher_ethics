package com.example.service.test;

import com.example.dto.test.QuizResultDTO;
import com.example.mapper.test.quiz.*;
import com.example.mapper.test.report.*;
import com.example.pojo.PageBean;
import com.example.pojo.test.quiz.*;
import com.example.pojo.test.report.Report;
import com.example.utils.JsonUtil;
import com.example.utils.ThreadLocalUtil;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecordService {
    @Autowired
    private QuestionnaireMapper questionnaireMapper;
    @Autowired
    private QuestionMapper questionMapper;
    @Autowired
    private JudgmentQuestionMapper judgmentMapper;
    @Autowired
    private ChoiceQuestionMapper choiceMapper;
    @Autowired
    private EssayQuestionMapper essayMapper;
    @Autowired
    private AnswerRecordMapper answerRecordMapper;

    @Autowired
    private ReportMapper reportMapper;
    @Autowired
    private ReportItemMapper srMapper;
    @Autowired
    private ReportEvidenceMapper evidenceMapper;
    @Autowired
    private ReportSuggestionMapper suggestionMapper;
    @Autowired
    private ReportDimensionScoreMapper dimScoreMapper;

    private void checkOwner(Integer ownerUserId) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer uid = (Integer) map.get("id");
        if (uid == null || !Objects.equals(uid, ownerUserId)) {
            throw new SecurityException("无权限访问该答卷");
        }
    }

    public QuizResultDTO getResult(Integer qnId) {
        Questionnaire qn = questionnaireMapper.selectById(qnId);
        if (qn == null) throw new IllegalStateException("答卷不存在");
        checkOwner(qn.getUserId());

        List<Integer> ids = JsonUtil.parseIds(qn.getQuestionSequence());
        Map<Integer, Question> bmap = questionMapper.selectByIds(ids).stream()
                .collect(Collectors.toMap(Question::getId, x -> x));

        Map<Integer, JudgmentQuestion> jmap = judgmentMapper.selectByQuestionIds(ids).stream()
                .collect(Collectors.toMap(JudgmentQuestion::getQuestionId, x -> x, (a, b) -> a));
        Map<Integer, ChoiceQuestion> cmap = choiceMapper.selectByQuestionIds(ids).stream()
                .collect(Collectors.toMap(ChoiceQuestion::getQuestionId, x -> x, (a, b) -> a));
        Map<Integer, EssayQuestion> emap = essayMapper.selectByQuestionIds(ids).stream()
                .collect(Collectors.toMap(EssayQuestion::getQuestionId, x -> x, (a, b) -> a));

        Map<Integer, AnswerRecord> amap = answerRecordMapper.listByQuestionnaireId(qnId).stream()
                .collect(Collectors.toMap(AnswerRecord::getQuestionId, x -> x, (a, b) -> a));

        QuizResultDTO dto = new QuizResultDTO();
        dto.setQnId(qnId);
        dto.setTitle(qn.getTitle());
        dto.setTotalScore(qn.getTotalScore());
        dto.setUserTotalScore(qn.getUserTotalScore());
        dto.setTimeSpent(qn.getTimeSpent());
        dto.setSubmittedAt(qn.getSubmittedAt());

        List<QuizResultDTO.QuestionResult> items = new ArrayList<>(ids.size());
        for (Integer id : ids) {
            Question b = bmap.get(id);
            if (b == null) continue;

            QuizResultDTO.QuestionResult item = new QuizResultDTO.QuestionResult();
            item.setId(b.getId());
            item.setType(b.getType());
            item.setContent(b.getContent());
            item.setDifficulty(b.getDifficulty());
            item.setScore(b.getScore());
            item.setAnalysis(b.getAnalysis());

            AnswerRecord ar = amap.get(id);
            if (ar != null) {
                item.setUserAnswer(JsonUtil.parseAny(ar.getUserAnswer()));
                item.setUserScore(ar.getUserScore());
                item.setIsMarked(ar.getIsMarked() != null && ar.getIsMarked() == 1);
            }

            if (b.getType() == 0) {
                JudgmentQuestion jq = jmap.get(id);
                item.setCorrectAnswer(jq != null && jq.getCorrectAnswer() == 1);
            } else if (b.getType() == 1) {
                ChoiceQuestion cq = cmap.get(id);
                item.setOptionA(cq.getOptionA());
                item.setOptionB(cq.getOptionB());
                item.setOptionC(cq.getOptionC());
                item.setOptionD(cq.getOptionD());
                item.setIsMultiple(cq.getIsMultiple() != null && cq.getIsMultiple() == 1);
                item.setCorrectAnswer(JsonUtil.parseListString(cq.getCorrectAnswer()));
            } else if (b.getType() == 2) {
                EssayQuestion eq = emap.get(id);
                if (eq != null) {
                    item.setReference(eq.getReference());
                    item.setKeyword(eq.getKeyword());
                }
            }

            items.add(item);
        }
        dto.setQuestionResults(items);
        return dto;
    }

    public List<Questionnaire> getRecent() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) throw new SecurityException("未登录");
        return questionnaireMapper.selectRecentByUserId(userId);
    }

    public PageBean<Questionnaire> list(Integer pageNum, Integer pageSize) {
        //1.创建PageBean对象
        PageBean<Questionnaire> pq = new PageBean<>();
        //2.开启分页查询PageHelper
        PageHelper.startPage(pageNum, pageSize);

        //3.调用mapper
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) throw new SecurityException("未登录");

        List<Questionnaire> list = questionnaireMapper.selectByUserId(userId);
        //Page中提供了方法，可以获取PageHelper分页查询后得到的总记录条数和当前页面数据
        Page<Questionnaire> p = (Page<Questionnaire>) list;
        //把数据填充到PageBean对象中
        pq.setTotal(p.getTotal());
        pq.setItems(p.getResult());
        return pq;
    }

    @Transactional
    public void deleteByQnId(Integer qnId) {
        Questionnaire qn = questionnaireMapper.selectById(qnId);
        if (qn == null) return;
        checkOwner(qn.getUserId());

        answerRecordMapper.deleteByQuestionnaireId(qnId);

        Report r = reportMapper.selectByQnId(qnId);
        if (r != null) {
            Integer rId = r.getId();
            evidenceMapper.deleteByReportId(rId);
            srMapper.deleteByReportId(rId);
            suggestionMapper.deleteByReportId(rId);
            dimScoreMapper.deleteByReportId(rId);
            reportMapper.deleteById(rId);
        }

        questionnaireMapper.deleteById(qnId);
    }
}
