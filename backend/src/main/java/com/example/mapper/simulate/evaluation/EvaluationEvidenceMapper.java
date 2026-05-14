package com.example.mapper.simulate.evaluation;

import com.example.pojo.simulate.evaluation.EvaluationEvidence;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface EvaluationEvidenceMapper {
    @Insert("INSERT INTO evaluation_evidences(item_id, evaluation_id, message_id, reason, created_at)" +
            "VALUES(#{itemId}, #{evaluationId}, #{messageId}, #{reason}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(EvaluationEvidence e);

    @Delete("DELETE FROM valuation_evidences WHERE evaluation_id=#{evaluationId}")
    int deleteByEvaluationId(Integer evaluationId);

    @Select("SELECT * FROM evaluation_evidences WHERE item_id=#{itemId} ORDER BY id ASC")
    List<EvaluationEvidence> listByItemId(Integer itemId);
}