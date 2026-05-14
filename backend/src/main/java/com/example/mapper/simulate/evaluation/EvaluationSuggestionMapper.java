package com.example.mapper.simulate.evaluation;

import com.example.pojo.simulate.evaluation.EvaluationSuggestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvaluationSuggestionMapper {
    @Insert("INSERT INTO evaluation_suggestions(evaluation_id, dimension_id, type, title, content, priority, created_at) " +
            "values(#{evaluationId}, #{dimensionId}, #{type}, #{title}, #{content}, #{priority}, now())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(EvaluationSuggestion s);

    @Delete("delete from evaluation_suggestions where evaluation_id = #{evaluationId}")
    int deleteByEvaluationId(Integer evaluationId);

    @Select("SELECT * FROM evaluation_suggestions WHERE evaluation_id=#{evaluationId} ORDER BY priority DESC, id ASC")
    List<EvaluationSuggestion> listByEvaluationId(Integer evaluationId);
}