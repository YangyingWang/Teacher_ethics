package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.ChoiceQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChoiceQuestionMapper {
    @Insert("INSERT INTO choice_questions(question_id, option_A, option_B, option_C, option_D, is_multiple, correct_answer, created_at) " +
            "VALUES(#{questionId}, #{optionA}, #{optionB}, #{optionC}, #{optionD}, #{isMultiple}, #{correctAnswer}, NOW()) ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ChoiceQuestion cq);

    //选择题相关
    @Select("SELECT * FROM choice_questions WHERE question_id = #{questionId}")
    ChoiceQuestion selectByQuestionId(Integer questionId);
    List<ChoiceQuestion> selectByQuestionIds(@Param("questionIds") List<Integer> questionIds);
}
