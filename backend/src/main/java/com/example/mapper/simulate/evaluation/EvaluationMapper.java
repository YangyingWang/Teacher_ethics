package com.example.mapper.simulate.evaluation;

import com.example.pojo.simulate.evaluation.Evaluation;
import org.apache.ibatis.annotations.*;

import java.util.List;
import java.util.Map;

@Mapper
public interface EvaluationMapper {
    @Insert("INSERT INTO evaluations(user_id, session_id, status, created_at, updated_at) " +
            "VALUES(#{userId}, #{sessionId}, #{status}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Evaluation e);

    @Delete("DELETE FROM evaluations WHERE id=#{id}")
    int deleteById(Integer id);

    @Update("UPDATE evaluations SET status=#{status}, overall_score=#{overallScore}, overall_level=#{overallLevel}, " +
            "summary=#{summary}, style=#{style}, risk_level=#{riskLevel}, updated_at=NOW() WHERE id=#{id}")
    int updateFull(Evaluation e);

    @Select("SELECT * FROM evaluations WHERE user_id = #{userId} AND status = 2 " +
            "ORDER BY updated_at DESC LIMIT 3")
    List<Evaluation> selectRecentByUserId(Integer userId);

    @Select("SELECT * FROM evaluations WHERE user_id = #{userId} AND status = 2 ")
    List<Evaluation> selectByUserId(Integer userId);

    @Select("SELECT id, session_id AS sessionId, overall_score AS overallScore, overall_level AS overallLevel, summary, style, risk_level AS riskLevel, updated_at AS updatedAt FROM evaluations WHERE user_id = #{userId} AND status = 1 ORDER BY updated_at DESC")
    List<Map<String, Object>> selectHomeRowsByUserId(Integer userId);

    @Select("SELECT * FROM evaluations WHERE session_id = #{sessionId}")
    Evaluation selectBySessionId(Integer sessionId);

    @Select("SELECT * FROM evaluations WHERE id = #{id}")
    Evaluation selectById(Integer id);

}
