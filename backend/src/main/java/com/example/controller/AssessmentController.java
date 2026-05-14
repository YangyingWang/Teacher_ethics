package com.example.controller;

import com.example.dto.assessment.AssessmentHomeDTO;
import com.example.pojo.Result;
import com.example.service.AssessmentService;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/assessment")
public class AssessmentController {
    @Autowired
    private AssessmentService assessmentService;

    @GetMapping("/home")
    public Result<AssessmentHomeDTO> home(@RequestParam(required = false, defaultValue = "quarter") String timeRange,
                                          @RequestParam(required = false, defaultValue = "3m") String trendRange,
                                          @RequestParam(required = false, defaultValue = "department") String comparisonTarget) {
        return Result.success(assessmentService.getHomeData(timeRange, trendRange, comparisonTarget));
    }

    @PostMapping("/refresh")
    public Result<AssessmentHomeDTO> refresh(@RequestParam(required = false, defaultValue = "quarter") String timeRange,
                                             @RequestParam(required = false, defaultValue = "3m") String trendRange,
                                             @RequestParam(required = false, defaultValue = "department") String comparisonTarget) {
        return Result.success(assessmentService.refreshHomeData(timeRange, trendRange, comparisonTarget));
    }

    @PostMapping("/report")
    public Result<Map<String, Object>> generateReport(@RequestParam(required = false, defaultValue = "quarter") String timeRange) {
        return Result.success(assessmentService.generateReport(timeRange));
    }

    @GetMapping("/export")
    public void export(@RequestParam(required = false, defaultValue = "quarter") String timeRange,
                       @RequestParam(required = false, defaultValue = "3m") String trendRange,
                       @RequestParam(required = false, defaultValue = "department") String comparisonTarget,
                       HttpServletResponse response) throws Exception {
        assessmentService.exportExcel(timeRange, trendRange, comparisonTarget, response);
    }
}
