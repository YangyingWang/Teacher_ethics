package com.example.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

@Data
public class AdminOverviewVO {
    private AdminSummaryVO summary = new AdminSummaryVO();
    private List<NameValueVO> teacherTypeStats = new ArrayList<>();
    private List<NameValueVO> departmentStats = new ArrayList<>();
    private List<NameValueVO> moduleStats = new ArrayList<>();
    private List<RecentActivityVO> recentActivities = new ArrayList<>();
}
