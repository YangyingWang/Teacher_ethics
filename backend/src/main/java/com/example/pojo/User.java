package com.example.pojo;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.sql.Date;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    @NotNull
    private Integer id;
    private String username;
    @JsonIgnore
    private String password;

    private String realName;
    private Integer sex;
    private String identityCard;
    private Date birthday;
    private String email;

    @NotEmpty
    @Pattern(regexp = "^1[3|4|5|7|8][0-9]\\d{8}$")
    private String phone;

    private String userPic;
    private Integer departmentId;
    private String title;
    private Integer type;
    private Date hireDate;
    private String expertise;
    private Integer disciplineId;
    private String bio;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
