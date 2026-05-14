package com.example.service;

import com.example.mapper.UserMapper;
import com.example.pojo.User;
import com.example.utils.Md5Util;
import com.example.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.sql.Date;
import java.util.Map;

@Service
public class UserService {
    @Autowired
    private UserMapper userMapper;
    public User findByUserName(String username) {
        return userMapper.findByUserName(username);
    }
    public void register(String username, String password, String realName, Integer sex, String identityCard, Date birthday, String email, String phone) {
        String md5String = Md5Util.getMD5String(password);
        User user = new User();
        user.setUsername(username);
        user.setPassword(md5String);
        user.setRealName(realName);
        user.setIdentityCard(identityCard);
        user.setBirthday(birthday);
        user.setEmail(email);
        user.setPhone(phone);
        user.setSex(sex);
        userMapper.add(user);
    }
    public void update(User user) {
        user.setUpdatedAt(LocalDateTime.now());
        userMapper.update(user);
    }
    public void updateAvatar(String avatarUrl) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        userMapper.updateAvatar(avatarUrl, id);
    }
    public void updatePwd(String newPwd) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer id = (Integer) map.get("id");
        userMapper.updatePwd(Md5Util.getMD5String(newPwd),id);
    }
}