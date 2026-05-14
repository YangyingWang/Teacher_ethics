package com.example.mapper.test.report;

import com.example.pojo.test.report.ReportItem;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReportItemMapper {
    @Insert("INSERT INTO report_items(report_id, type, content, level, created_at)" +
            "VALUES(#{reportId}, #{type}, #{content}, #{level}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ReportItem r);

    @Delete("DELETE FROM report_items WHERE report_id=#{reportId}")
    int deleteByReportId(Integer reportId);

    @Select("SELECT * FROM report_items WHERE report_id=#{reportId} AND type=#{type} ORDER BY id ASC")
    List<ReportItem> listByReportAndType(Integer reportId, Integer type);
}
