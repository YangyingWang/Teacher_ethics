package com.example.mapper.test.report;

import com.example.pojo.test.report.ReportDimension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface ReportDimensionMapper {
    @Select("SELECT * FROM report_dimensions")
    List<ReportDimension> listAll();
}
