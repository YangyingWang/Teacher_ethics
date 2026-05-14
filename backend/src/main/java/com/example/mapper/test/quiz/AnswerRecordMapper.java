package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.AnswerRecord;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface AnswerRecordMapper {
    @Insert("INSERT INTO answer_records(questionnaire_id, question_id, user_answer, user_score, is_marked, created_at, updated_at) " +
            "VALUES(#{questionnaireId}, #{questionId}, #{userAnswer}, #{userScore}, #{isMarked}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(AnswerRecord r);

    int batchInsert(@Param("records") List<AnswerRecord> records);

    @Delete("DELETE FROM answer_records WHERE questionnaire_id=#{questionnaireId}")
    int deleteByQuestionnaireId(Integer questionnaireId);

    @Select("SELECT * FROM answer_records WHERE questionnaire_id = #{questionnaireId}")
    List<AnswerRecord> listByQuestionnaireId(Integer questionnaireId);
}
