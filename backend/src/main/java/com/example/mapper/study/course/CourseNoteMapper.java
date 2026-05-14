package com.example.mapper.study.course;

import com.example.pojo.study.course.CourseNote;
import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseNoteMapper {

    @Select("SELECT * FROM course_note WHERE user_id = #{userId} AND course_id = #{courseId} ORDER BY updated_at DESC, id DESC")
    List<CourseNote> listByUserIdAndCourseId(Integer userId, Integer courseId);

    @Select("SELECT * FROM course_note WHERE id = #{id}")
    CourseNote selectById(Integer id);

    @Insert("""
            INSERT INTO course_note(user_id, course_id, content, created_at, updated_at)
            VALUES(#{userId}, #{courseId}, #{content}, NOW(), NOW())
            """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    int insert(CourseNote note);

    @Update("""
            UPDATE course_note
            SET content = #{content},
                updated_at = NOW()
            WHERE id = #{id}
              AND user_id = #{userId}
            """)
    int update(CourseNote note);

    @Delete("DELETE FROM course_note WHERE id = #{id} AND user_id = #{userId}")
    int deleteByIdAndUserId(Integer id, Integer userId);
}
