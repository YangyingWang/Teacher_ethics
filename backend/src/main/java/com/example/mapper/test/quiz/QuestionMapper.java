package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.Question;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionMapper {
    @Insert("INSERT INTO questions(scene_id,type,content, analysis, difficulty, score, created_at) " +
            "VALUES(#{sceneId},#{type}, #{content}, #{analysis}, #{difficulty}, #{score}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Question q);

    //批量获取题目
    List<Question> selectByIds(@Param("ids") List<Integer> ids);

    //根据ID获取题目
    @Select("SELECT * FROM questions WHERE id = #{id}")
    Question selectById(Integer id);

    //根据场景ID获取题目列表
    @Select("SELECT * FROM questions WHERE scene_id = #{sceneId}")
    List<Question> selectQuestionsBySceneId(Integer sceneId);
}
