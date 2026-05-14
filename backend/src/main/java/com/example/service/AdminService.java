package com.example.service;

import com.example.mapper.AdminMapper;
import com.example.pojo.Admin;
import com.example.utils.ThreadLocalUtil;
import com.example.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class AdminService {
    @Autowired
    private AdminMapper adminMapper;

    public Admin findByUsername(String username) {
        return adminMapper.findByUsername(username);
    }

    public AdminOverviewVO getOverview() {
        ensureAdmin();

        AdminOverviewVO overviewVO = new AdminOverviewVO();
        AdminSummaryVO summaryVO = adminMapper.getSummary();
        if (summaryVO == null) {
            summaryVO = new AdminSummaryVO();
            summaryVO.setTeacherTotal(0);
            summaryVO.setCourseLearnerCount(0);
            summaryVO.setSimulationParticipantCount(0);
            summaryVO.setEvaluationCount(0);
            summaryVO.setAssessmentCount(0);
            summaryVO.setAvgAssessmentScore(BigDecimal.ZERO);
        }

        overviewVO.setSummary(summaryVO);
        overviewVO.setTeacherTypeStats(adminMapper.getTeacherTypeStats());
        overviewVO.setDepartmentStats(adminMapper.getDepartmentStats());
        overviewVO.setModuleStats(adminMapper.getModuleStats());
        overviewVO.setRecentActivities(adminMapper.getRecentActivities());
        return overviewVO;
    }

    public Map<String, Object> page(Integer pageNum, Integer pageSize, String username, String realName, Integer type, Integer depId) {
        int safePageNum = (pageNum == null || pageNum < 1) ? 1 : pageNum;
        int safePageSize = (pageSize == null || pageSize < 1) ? 10 : pageSize;
        int offset = (safePageNum - 1) * safePageSize;

        String usernameLike = StringUtils.hasText(username) ? username.trim() : null;
        String realNameLike = StringUtils.hasText(realName) ? realName.trim() : null;

        Integer total = adminMapper.countTeachers(usernameLike, realNameLike, type, depId);
        List<AdminTeacherPageVO> items = adminMapper.pageTeachers(usernameLike, realNameLike, type, depId, offset, safePageSize);

        Map<String, Object> result = new HashMap<>();
        result.put("total", total == null ? 0 : total);
        result.put("items", items);
        return result;
    }


    public AdminTeacherDetailVO detail(Integer id) {
        return adminMapper.getTeacherDetail(id);
    }

    public List<NameValueVO> departments() {
        return adminMapper.listDepartments();
    }


    public AdminStatisticsOverviewVO getOverview(Integer type, Integer depId) {
        AdminStatisticsOverviewVO vo = new AdminStatisticsOverviewVO();
        AdminStatisticsSummaryVO summary = adminMapper.getStatisticsSummary(type, depId);
        if (summary == null) {
            summary = new AdminStatisticsSummaryVO();
        }
        vo.setSummary(summary);
        vo.setTeacherTypeStats(adminMapper.getTeacherTypeStatsByDepId(depId));
        vo.setDepartmentStats(adminMapper.getDepartmentStatsByType(type));
        vo.setLevelStats(adminMapper.getLevelStats(type, depId));
        vo.setAssessmentTrend(adminMapper.getAssessmentTrend(type, depId));
        vo.setRegisterTrend(adminMapper.getRegisterTrend(type, depId));
        vo.setSimulationStats(adminMapper.getSimulationStats(type, depId));
        return vo;
    }

    private void ensureAdmin() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        if (claims == null) {
            throw new RuntimeException("未获取到登录信息");
        }
        Object roleObj = claims.get("role");
        String role = roleObj == null ? "" : roleObj.toString();
        if (!"admin".equals(role)) {
            throw new RuntimeException("无权限访问管理端数据");
        }
    }
}
