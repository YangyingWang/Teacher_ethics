package com.example.mapper.test.report;

import com.example.pojo.test.report.ReportEvidence;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ReportEvidenceMapper {
    @Insert("INSERT INTO report_evidences(item_id, report_id, question_id, reason, created_at)" +
            "VALUES(#{itemId}, #{reportId}, #{questionId}, #{reason}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(ReportEvidence e);

    @Delete("DELETE FROM report_evidences WHERE report_id=#{reportId}")
    int deleteByReportId(Integer reportId);

    @Select("SELECT * FROM report_evidences WHERE item_id=#{srId} ORDER BY id ASC")
    List<ReportEvidence> listByItemId(Integer itemId);
}
