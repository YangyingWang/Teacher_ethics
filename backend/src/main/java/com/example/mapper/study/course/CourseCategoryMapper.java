package com.example.mapper.study.course;

import com.example.dto.study.course.CourseHomeDTO;
import com.example.pojo.study.course.CourseCategory;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface CourseCategoryMapper {
    @Select("SELECT * FROM course_category ORDER BY id ASC")
    List<CourseCategory> listAll();

    @Select("""
        SELECT
            cc.id AS id,
            cc.name AS name,
            cc.description AS description,
            COALESCE(c1.course_count, 0) AS courseCount,
            COALESCE(c2.study_hours, 0) AS studyHours
        FROM course_category cc
        LEFT JOIN (
            SELECT category_id, COUNT(*) AS course_count
            FROM courses
            WHERE status = 1
            GROUP BY category_id
        ) c1 ON c1.category_id = cc.id
        LEFT JOIN (
            SELECT
                category_id,
                ROUND(COALESCE(SUM(duration), 0) / 3600.0, 1) AS study_hours
            FROM courses
            WHERE status = 1
            GROUP BY category_id
        ) c2 ON c2.category_id = cc.id
        ORDER BY cc.id ASC
        """)
    List<CourseHomeDTO.CategoryCard> listCategoryCards();
}
