package com.example.mapper.study.course;

import com.example.dto.study.course.CourseDetailDTO;
import com.example.dto.study.course.CourseHomeDTO;
import com.example.pojo.study.course.Course;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface CourseMapper {

    @Select("SELECT type FROM users WHERE id = #{userId}")
    Integer selectTeacherTypeByUserId(Integer userId);

    @Select("SELECT * FROM courses WHERE id = #{id}")
    Course selectById(Integer id);

    @Select("""
            SELECT
                c.id AS id,
                c.title AS title,
                c.description AS description,
                c.cover_url AS cover,
                c.video_url AS videoUrl,
                c.difficulty AS difficulty,
                c.category_id AS categoryId,
                cc.name AS categoryName,
                c.duration AS duration,
                c.enrollment_count AS enrollment,
                c.hot_score AS hotScore,
                ROUND(COALESCE(cl.progress_percent, 0)) AS progress,
                CASE WHEN cf.id IS NULL THEN FALSE ELSE TRUE END AS favorite
            FROM courses c
            LEFT JOIN course_category cc ON cc.id = c.category_id
            LEFT JOIN course_learning cl ON cl.course_id = c.id AND cl.user_id = #{userId}
            LEFT JOIN course_favorite cf ON cf.course_id = c.id AND cf.user_id = #{userId}
            WHERE c.status = 1
              AND c.is_featured = 1
              AND (#{teacherType} IS NULL OR c.teacher_type IS NULL OR c.teacher_type = #{teacherType})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR c.title LIKE CONCAT('%', #{keyword}, '%')
                   OR c.description LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{difficulty} IS NULL OR c.difficulty = #{difficulty})
              AND (#{categoryId} IS NULL OR c.category_id = #{categoryId})
            ORDER BY c.hot_score DESC, c.created_at DESC
            LIMIT #{limit}
            """)
    List<CourseHomeDTO.CourseCard> listFeaturedCourses(Integer userId, Integer teacherType,
                                                       String keyword, Integer difficulty,
                                                       Integer categoryId, Integer limit);

    @Select("""
            SELECT
                c.id AS id,
                c.title AS title,
                c.description AS description,
                c.cover_url AS cover,
                c.video_url AS videoUrl,
                c.difficulty AS difficulty,
                c.category_id AS categoryId,
                cc.name AS categoryName,
                c.duration AS duration,
                c.enrollment_count AS enrollment,
                c.hot_score AS hotScore,
                ROUND(COALESCE(cl.progress_percent, 0)) AS progress,
                CASE WHEN cf.id IS NULL THEN FALSE ELSE TRUE END AS favorite
            FROM courses c
            LEFT JOIN course_category cc ON cc.id = c.category_id
            LEFT JOIN course_learning cl ON cl.course_id = c.id AND cl.user_id = #{userId}
            LEFT JOIN course_favorite cf ON cf.course_id = c.id AND cf.user_id = #{userId}
            WHERE c.status = 1
              AND (#{teacherType} IS NULL OR c.teacher_type IS NULL OR c.teacher_type = #{teacherType})
              AND (#{keyword} IS NULL OR #{keyword} = ''
                   OR c.title LIKE CONCAT('%', #{keyword}, '%')
                   OR c.description LIKE CONCAT('%', #{keyword}, '%'))
              AND (#{difficulty} IS NULL OR c.difficulty = #{difficulty})
              AND (#{categoryId} IS NULL OR c.category_id = #{categoryId})
            ORDER BY c.hot_score DESC, c.enrollment_count DESC, c.created_at DESC
            LIMIT #{limit}
            """)
    List<CourseHomeDTO.CourseCard> listPopularCourses(Integer userId, Integer teacherType,
                                                      String keyword, Integer difficulty,
                                                      Integer categoryId, Integer limit);

    @Select("""
            SELECT
                c.id AS id,
                c.title AS title,
                c.description AS description,
                c.cover_url AS cover,
                c.video_url AS videoUrl,
                c.difficulty AS difficulty,
                c.category_id AS categoryId,
                cc.name AS categoryName,
                c.duration AS duration,
                c.enrollment_count AS enrollment,
                c.status AS status,
                ROUND(COALESCE(cl.progress_percent, 0)) AS progress,
                COALESCE(cl.last_sec, 0) AS lastSec,
                COALESCE(cl.study_total, 0) AS studyTotal,
                cl.last_time AS lastStudyTime,
                CASE WHEN cf.id IS NULL THEN FALSE ELSE TRUE END AS favorite
            FROM courses c
            LEFT JOIN course_category cc ON cc.id = c.category_id
            LEFT JOIN course_learning cl ON cl.course_id = c.id AND cl.user_id = #{userId}
            LEFT JOIN course_favorite cf ON cf.course_id = c.id AND cf.user_id = #{userId}
            WHERE c.id = #{courseId}
              AND c.status = 1
            LIMIT 1
            """)
    CourseDetailDTO selectCourseDetail(Integer userId, Integer courseId);

    @Update("UPDATE courses SET enrollment_count = enrollment_count + 1 WHERE id = #{courseId}")
    int incrementEnrollmentCount(Integer courseId);
}
