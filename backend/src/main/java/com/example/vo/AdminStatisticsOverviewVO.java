package com.example.vo;

import lombok.Data;

import java.util.List;

@Data
public class AdminStatisticsOverviewVO {
    private AdminStatisticsSummaryVO summary;
    private List<NameValueVO> teacherTypeStats;
    private List<NameValueVO> departmentStats;
    private List<NameValueVO> levelStats;
    private List<NameValueVO> assessmentTrend;
    private List<NameValueVO> registerTrend;
    private List<NameValueVO> simulationStats;
}
