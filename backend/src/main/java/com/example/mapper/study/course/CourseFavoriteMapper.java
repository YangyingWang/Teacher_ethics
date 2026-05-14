package com.example.mapper.study.course;

import com.example.pojo.study.course.CourseFavorite;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface CourseFavoriteMapper {

    @Select("SELECT * FROM course_favorite WHERE user_id = #{userId} AND course_id = #{courseId}")
    CourseFavorite selectByUserIdAndCourseId(Integer userId, Integer courseId);

    @Insert("INSERT INTO course_favorite(user_id, course_id, created_at) VALUES(#{userId}, #{courseId}, NOW())")
    int insert(CourseFavorite favorite);

    @Delete("DELETE FROM course_favorite WHERE user_id = #{userId} AND course_id = #{courseId}")
    int deleteByUserIdAndCourseId(Integer userId, Integer courseId);
}
