package com.example.mapper.simulate.evaluation;

import com.example.pojo.simulate.evaluation.EvaluationItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvaluationItemMapper {
    @Insert("INSERT INTO evaluation_items(evaluation_id, type, dimension_id, content, round_no, level, created_at)" +
            "VALUES(#{evaluationId}, #{type}, #{dimensionId}, #{content}, #{roundNo}, #{level}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(EvaluationItem item);

    @Delete("DELETE FROM evaluation_items WHERE evaluation_id=#{evaluationId}")
    int deleteByEvaluationId(Integer evaluationId);

    @Select("SELECT * FROM evaluation_items WHERE evaluation_id=#{evaluationId} AND type=#{type} ORDER BY id ASC")
    List<EvaluationItem> listByEvaluationIdAndType(Integer evaluationId, Integer type);
}
