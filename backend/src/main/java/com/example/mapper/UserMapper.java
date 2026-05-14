package com.example.mapper;

import com.example.pojo.User;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface UserMapper {
    @Select("select * from users where username=#{username}")
    User findByUserName(String username);

    @Insert("insert into users(username,password,real_name,sex,identity_card,birthday," +
            "email,phone,created_at,updated_at) values (#{username},#{password},#{realName}," +
            "#{sex},#{identityCard},#{birthday},#{email},#{phone},now(),now())")
    void add(User user);

    @Update("update users set " +
            "sex=#{sex},identity_card=#{identityCard},birthday=#{birthday}," +
            "email=#{email},phone=#{phone},title=#{title},type=#{type},hire_date=#{hireDate},expertise=#{expertise}," +
            "bio=#{bio},updated_at=#{updatedAt} where id=#{id}")
    void update(User user);

    @Update("update users set user_pic=#{avatarUrl},updated_at=now() where id=#{id}")
    void updateAvatar(String avatarUrl, Integer id);

    @Update("update users set password=#{md5String},updated_at=now() where id=#{id}")
    void updatePwd(String md5String, Integer id);
}
