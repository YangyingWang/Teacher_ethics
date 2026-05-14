package com.example.vo;

import lombok.Data;

@Data
public class AdminTeacherDetailVO {
    private Integer id;
    private String username;
    private String realName;
    private Integer sex;
    private String phone;
    private String email;
    private String title;
    private Integer type;
    private Integer depId;
    private String depName;
    private String hireDate;
    private String createdAt;
    private String expertise;
    private String bio;
    private Integer assessmentCount;
    private Integer latestAssessmentScore;
    private Integer simulationCount;
}
