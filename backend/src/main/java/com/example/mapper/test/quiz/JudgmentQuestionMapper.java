package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.JudgmentQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface JudgmentQuestionMapper {
    @Insert("INSERT INTO judgment_questions(question_id, correct_answer, created_at) " +
            "VALUES(#{questionId}, #{correctAnswer}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(JudgmentQuestion jq);

    @Select("SELECT * FROM judgment_questions WHERE question_id = #{questionId}")
    JudgmentQuestion selectByQuestionId(Integer questionId);

    List<JudgmentQuestion> selectByQuestionIds(@Param("questionIds") List<Integer> questionIds);
}
