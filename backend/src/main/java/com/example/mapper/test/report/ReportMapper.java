package com.example.mapper.test.report;

import com.example.pojo.test.report.Report;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReportMapper {
    @Insert("INSERT INTO reports(user_id, questionnaire_id, scene_id, status, code, created_at, updated_at) " +
            "VALUES(#{userId}, #{questionnaireId}, #{sceneId}, #{status}, #{code}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Report r);

    @Delete("DELETE FROM reports WHERE id=#{id}")
    int deleteById(Integer id);

    @Update("UPDATE reports SET status=#{status}, summary=#{summary}, raw_json=#{rawJson}, updated_at=NOW() WHERE id=#{id}")
    int updateFull(Report r);

    @Update("UPDATE reports SET status=#{status}, total_score=#{totalScore}, user_total_score=#{userTotalScore}, time_spent=#{timeSpent}," +
            "overall_score=#{overallScore}, overall_level=#{overallLevel}, updated_at=NOW() WHERE id=#{id}")
    int updatePartial(Report r);

    @Select("SELECT * FROM reports WHERE user_id = #{userId} AND status = 2 " +
            "ORDER BY updated_at DESC LIMIT 3")
    List<Report> selectRecentByUserId(Integer userId);

    @Select("SELECT * FROM reports WHERE user_id = #{userId} AND status = 2 ")
    List<Report> selectByUserId(Integer userId);

    @Select("SELECT * FROM reports WHERE questionnaire_id=#{qnId}")
    Report selectByQnId(Integer qnId);

    @Select("SELECT * FROM reports WHERE id=#{id}")
    Report selectById(Integer  id);
}
