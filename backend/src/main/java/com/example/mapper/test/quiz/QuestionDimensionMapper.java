package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.QuestionDimension;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface QuestionDimensionMapper {
    @Insert("INSERT INTO question_dimensions(question_id, dimension_id, weight, created_at)" +
            "VALUES(#{questionId}, #{dimensionId}, #{weight}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(QuestionDimension qd);

    @Select("SELECT * FROM question_dimensions WHERE question_id=#{questionId}")
    List<QuestionDimension> selectByQuestionId(Integer questionId);
}
