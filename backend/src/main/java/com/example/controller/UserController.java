package com.example.controller;

import cn.hutool.captcha.CaptchaUtil;
import cn.hutool.captcha.ShearCaptcha;
import com.example.pojo.Admin;
import com.example.pojo.Result;
import com.example.pojo.User;
import com.example.service.AdminService;
import com.example.service.UserService;
import com.example.utils.JwtUtil;
import com.example.utils.Md5Util;
import com.example.utils.ThreadLocalUtil;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.Pattern;
import org.hibernate.validator.constraints.URL;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.util.StringUtils;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/user")
@Validated
public class UserController {
    @Autowired
    private UserService userService;
    @Autowired
    private AdminService adminService;

    @PostMapping("/register")
    //需要进行参数校验
    public Result register(@Pattern(regexp = "^\\S{7}$") String username,
                           @Pattern(regexp = "^\\S{6,16}$") String password,
                           String realName,
                           String identityCard,
                           String birthday,
                           @RequestParam Integer sex,
                           String email,
                           String phone) {
        User u = userService.findByUserName(username);
        if (u == null) {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
            java.util.Date birthDate = null;
            try {
                birthDate = sdf.parse(birthday);
            } catch (Exception e) {
                return Result.error("生日格式不正确，请使用YYYY-MM-DD格式！");
            }
            java.sql.Date birth = new java.sql.Date(birthDate.getTime());
            userService.register(username, password, realName, sex, identityCard, birth, email, phone);
            return Result.success();
        } else {
            return Result.error("该账号已注册，请返回登录！");
        }
    }

    @PostMapping("/login")
    public Result<Map<String, Object>> login(@Pattern(regexp = "^\\S{2,20}$") String username,
                                @Pattern(regexp = "^\\S{6,16}$") String password,
                                @RequestParam String verCode,
                                HttpSession session) {
        // 校验验证码
        String sessionCaptcha = (String) session.getAttribute("captcha");
        if (sessionCaptcha == null || !sessionCaptcha.equalsIgnoreCase(verCode)) {
            return Result.error("验证码错误！");
        }
        // 先查看是否为管理员
        Admin admin = adminService.findByUsername(username);
        if (admin != null) {
            if (admin.getStatus() != null && admin.getStatus() == 0) {
                return Result.error("管理员账号已被禁用！");
            }
            if (Md5Util.getMD5String(password).equals(admin.getPassword())) {
                Map<String, Object> claims = new HashMap<>();
                claims.put("id", admin.getId());
                claims.put("username", admin.getUsername());
                claims.put("role", "admin");
                String token = JwtUtil.genToken(claims);

                Map<String, Object> data = new HashMap<>();
                data.put("token", token);
                data.put("role", "admin");
                data.put("username", admin.getUsername());
                data.put("realName", admin.getRealName());
                return Result.success(data);
            }
            return Result.error("密码错误！");
        }

        //根据用户名查询用户
        User loginUser = userService.findByUserName(username);
        if (loginUser == null) {
            return Result.error("该账号不存在！");
        }
        //判断密码是否正确(注意：LoginUser对象中的password是密文)
        if (Md5Util.getMD5String(password).equals(loginUser.getPassword())) {
            //登录成功
            Map<String, Object> claims = new HashMap<>();
            claims.put("id", loginUser.getId());
            claims.put("username", loginUser.getUsername());
            claims.put("role", "teacher");
            String token = JwtUtil.genToken(claims);

            Map<String, Object> data = new HashMap<>();
            data.put("token", token);
            data.put("role", "teacher");
            data.put("username", loginUser.getUsername());
            data.put("realName", loginUser.getRealName());
            return Result.success(data);
        }
        return Result.error("密码错误！");
    }
    @GetMapping("/captcha")
    public Map<String, Object> getCaptcha(HttpSession session) {
        // 创建验证码（4位）
        ShearCaptcha captcha = CaptchaUtil.createShearCaptcha(120, 30, 4, 3);
        String code = captcha.getCode();
        String base64 = "data:image/png;base64," + cn.hutool.core.codec.Base64.encode(captcha.getImageBytes());

        session.setAttribute("captcha", code.toLowerCase());
        return Map.of("imgBase64", base64); // 返回图片的 Base64 编码
    }

    @GetMapping("/userInfo")
    public Result<User> userInfo(){
        Map<String,Object> map = ThreadLocalUtil.get();
        String username = (String)map.get("username");
        User user=userService.findByUserName(username);
        return Result.success(user);
    }

    @PutMapping("/userUpdate")
    public Result update(@RequestBody @Validated User user){
        userService.update(user);
        return Result.success();
    }

    @PatchMapping("/updateAvatar")
    public Result updateAvatar(@RequestParam @URL String avatarUrl){
        userService.updateAvatar(avatarUrl);
        return Result.success();
    }

    @PatchMapping("/updatePwd")
    public Result updatePwd(@RequestBody Map<String,String> params){
        //1.校验参数
        String oldPwd = params.get("oldPwd");
        String newPwd = params.get("newPwd");
        String rePwd = params.get("rePwd");

        if(!StringUtils.hasLength(oldPwd) || !StringUtils.hasLength(newPwd) || !StringUtils.hasLength(rePwd)){
            return Result.error("缺少必要的参数！");
        }
        //调用userService根据用户名拿到原密码，再和oldPwd比对
        Map<String,Object> map =ThreadLocalUtil.get();
        String username = (String) map.get("username");
        User loginUser = userService.findByUserName(username);
        if(!loginUser.getPassword().equals(Md5Util.getMD5String(oldPwd))){
            return Result.error("原密码填写不正确！");
        }
        if(!rePwd.equals(newPwd)){
            return Result.error("两次填写的密码不一致！");
        }
        if(oldPwd.equals(newPwd)){
            return Result.error("新密码和原密码一致！");
        }
        //2.调用service完成密码更新
        userService.updatePwd(newPwd);
        return Result.success();
    }
}