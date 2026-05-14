package com.example.mapper.test.report;

import com.example.pojo.test.report.ReportDimensionScore;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReportDimensionScoreMapper {
    @Insert("INSERT INTO report_dimension_scores(dimension_id, report_id, score, raw_score, raw_total, question_count, weight, wrong_count, low_count, created_at) " +
            "VALUES(#{dimensionId}, #{reportId}, #{score}, #{rawScore}, #{rawTotal}, #{questionCount}, #{weight}, #{wrongCount}, #{lowCount}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ReportDimensionScore s);

    @Delete("DELETE FROM report_dimension_scores WHERE report_id=#{reportId}")
    int deleteByReportId(Integer reportId);

    @Select("SELECT * FROM report_dimension_scores WHERE report_id=#{reportId} ORDER BY dimension_id ASC")
    List<ReportDimensionScore> listByReportId(Integer reportId);
}
