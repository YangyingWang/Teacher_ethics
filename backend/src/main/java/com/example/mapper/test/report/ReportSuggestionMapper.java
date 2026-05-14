package com.example.mapper.test.report;

import com.example.pojo.test.report.ReportSuggestion;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReportSuggestionMapper {
    @Insert("INSERT INTO report_suggestions(report_id, dimension_id, title, content, priority, created_at) " +
            "VALUES(#{reportId}, #{dimensionId}, #{title}, #{content}, #{priority}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ReportSuggestion s);

    @Delete("DELETE FROM report_suggestions WHERE report_id=#{reportId}")
    int deleteByReportId(Integer reportId);

    @Select("SELECT * FROM report_suggestions WHERE report_id=#{reportId} ORDER BY priority DESC, id ASC")
    List<ReportSuggestion> listByReport(Integer reportId);
}
