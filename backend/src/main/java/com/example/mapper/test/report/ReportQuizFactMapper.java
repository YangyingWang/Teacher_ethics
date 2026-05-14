package com.example.mapper.test.report;

import com.example.dto.test.ReportQuizFactDTO;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportQuizFactMapper {
    @Select("SELECT q.id AS qId, q.type AS type, q.content AS content, q.analysis AS analysis, " +
                "q.score AS fullScore, ar.user_score AS userScore, ar.user_answer AS userAnswer, " +
                "cq.is_multiple AS isMultiple, cq.correct_answer AS correctAnswers, " +
                "jq.correct_answer AS correctAnswerTF, " +
                "eq.reference AS reference, eq.keyword AS keyword " +
            "FROM answer_records ar " +
            "JOIN questions q ON ar.question_id = q.id " +
            "LEFT JOIN choice_questions cq ON cq.question_id = q.id " +
            "LEFT JOIN judgment_questions jq ON jq.question_id = q.id " +
            "LEFT JOIN essay_questions eq ON eq.question_id = q.id " +
            "WHERE ar.questionnaire_id = #{qnId} ORDER BY q.id ASC ")
    List<ReportQuizFactDTO> listFactsByQnId(Integer qnId);
}
