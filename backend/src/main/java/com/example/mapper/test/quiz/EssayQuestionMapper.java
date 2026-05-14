package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.EssayQuestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EssayQuestionMapper {
    @Insert("INSERT INTO essay_questions(question_id, reference, keyword, created_at) " +
            "VALUES(#{questionId}, #{reference}, #{keyword}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(EssayQuestion eq);

    //简答题相关
    @Select("SELECT * FROM essay_questions WHERE question_id = #{questionId}")
    EssayQuestion selectByQuestionId(Integer questionId);
    List<EssayQuestion> selectByQuestionIds(@Param("questionIds") List<Integer> questionIds);
}
