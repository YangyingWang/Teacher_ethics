package com.example.mapper.simulate.evaluation;

import com.example.pojo.simulate.evaluation.EvaluationDimensionScore;
import com.example.pojo.test.report.ReportDimensionScore;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvaluationDimensionScoreMapper {
    @Insert("INSERT INTO evaluation_dimension_scores(dimension_id, evaluation_id, score, weight, comment, created_at) " +
            "VALUES(#{dimensionId}, #{evaluationId}, #{score}, #{weight}, #{comment}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(EvaluationDimensionScore s);

    @Delete("DELETE FROM evaluation_dimension_scores WHERE evaluation_id=#{evaluationId}")
    int deleteByEvaluationId(Integer evaluationId);

    @Select("SELECT * FROM evaluation_dimension_scores WHERE evaluation_id=#{evaluationId} ORDER BY dimension_id ASC")
    List<EvaluationDimensionScore> listByEvaluationId(Integer evaluationId);
}
