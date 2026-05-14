package com.example.controller;

import com.example.pojo.Admin;
import com.example.pojo.Result;
import com.example.service.AdminService;
import com.example.utils.ThreadLocalUtil;
import com.example.vo.AdminOverviewVO;
import com.example.vo.AdminStatisticsOverviewVO;
import com.example.vo.AdminTeacherDetailVO;
import com.example.vo.NameValueVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/admin")
public class AdminController {
    @Autowired
    private AdminService adminService;

    @GetMapping("/info")
    public Result<Admin> info() {
        Map<String, Object> map = ThreadLocalUtil.get();
        String username = (String) map.get("username");
        Admin admin = adminService.findByUsername(username);
        return Result.success(admin);
    }

    @GetMapping("/overview")
    public Result<AdminOverviewVO> getOverview() {
        return Result.success(adminService.getOverview());
    }

    @GetMapping("/teachers/page")
    public Result<Map<String, Object>> page(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize,
                                            @RequestParam(required = false) String username,
                                            @RequestParam(required = false) String realName,
                                            @RequestParam(required = false) Integer type,
                                            @RequestParam(required = false) Integer depId) {
        Map<String, Object> data = adminService.page(pageNum, pageSize, username, realName, type, depId);
        return Result.success(data);
    }

    @GetMapping("/teachers/{id}")
    public Result<AdminTeacherDetailVO> detail(@PathVariable Integer id) {
        return Result.success(adminService.detail(id));
    }

    @GetMapping("/teachers/departments")
    public Result<List<NameValueVO>> departments() {
        return Result.success(adminService.departments());
    }


    @GetMapping("/statistics")
    public Result<AdminStatisticsOverviewVO> getOverview(@RequestParam(required = false) Integer type,
                                                         @RequestParam(required = false) Integer depId) {
        return Result.success(adminService.getOverview(type, depId));
    }

}