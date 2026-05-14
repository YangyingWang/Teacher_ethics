package com.example.controller.test;

import com.example.dto.test.ReportDetailDTO;
import com.example.pojo.PageBean;
import com.example.pojo.Result;
import com.example.pojo.test.report.Report;
import com.example.pojo.test.report.ReportDimension;
import com.example.service.test.ReportService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test/report")
public class ReportController {
    @Autowired
    private ReportService reportService;

    @PostMapping
    public Result<Report> full(Integer qnId) {
        Report r = reportService.ensureFull(qnId);
        return Result.success(r);
    }

    // 详情（ReportDetail.vue 用）
    @GetMapping
    public Result<ReportDetailDTO> detail(Integer rId) {
        return Result.success(reportService.detail(rId));
    }

    @GetMapping("/dimensions")
    public Result<List<ReportDimension>> dimensions(){
        List<ReportDimension> dss = reportService.getDimensions();
        return Result.success(dss);
    }

    @GetMapping("/recent")
    public Result<List<Report>> recent() {
        return Result.success(reportService.getRecent());
    }

    @GetMapping("/list")
    public Result<PageBean<Report>> list(Integer pageNum, Integer pageSize) {
        PageBean<Report> pq = reportService.list(pageNum, pageSize);
        return Result.success(pq);
    }
}
