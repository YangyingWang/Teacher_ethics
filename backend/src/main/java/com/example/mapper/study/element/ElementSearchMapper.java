package com.example.mapper.study.element;

import com.example.dto.study.element.*;
import com.example.pojo.study.element.CourseType;
import com.example.pojo.study.element.Discipline;
import com.example.pojo.study.element.ElementType;
import com.example.pojo.study.element.TeachingCourse;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface ElementSearchMapper {

    @Select("SELECT id, name, status, created_at, updated_at FROM element_types WHERE status = 1")
    List<ElementType> listEnabledElementTypes();

    @Select("SELECT id, name, status, created_at, updated_at FROM disciplines WHERE status = 1")
    List<Discipline> listEnabledDisciplines();

    @Select("SELECT id, name, status, created_at, updated_at FROM course_types WHERE status = 1 ")
    List<CourseType> listEnabledCourseTypes();

    @Select("""
            SELECT id, name, discipline_id, course_type_id, status, created_at, updated_at
            FROM teaching_courses
            WHERE status = 1
              AND (#{disciplineId} IS NULL OR discipline_id = #{disciplineId})
            ORDER BY id ASC
            """)
    List<TeachingCourse> listTeachingCoursesByDiscipline(Integer disciplineId);

    @Select("SELECT id, type, discipline_id FROM users WHERE id = #{userId} LIMIT 1")
    SearchUserProfile selectUserProfile(Integer userId);

    @Select("SELECT COUNT(*) FROM elements WHERE status = 1")
    Integer countEnabledElements();

    @Select("SELECT COUNT(*) FROM teaching_courses WHERE status = 1")
    Integer countEnabledTeachingCourses();

    @Select("""
            SELECT keywords
            FROM elements
            WHERE status = 1
              AND keywords IS NOT NULL
              AND keywords <> ''
            ORDER BY hot_score DESC, id DESC
            LIMIT #{limit}
            """)
    List<String> listHotKeywordSources(Integer limit);

    @Select("""
            <script>
            SELECT COUNT(DISTINCT e.id)
            FROM elements e
            LEFT JOIN elements_disciplines ed ON e.id = ed.element_id
            LEFT JOIN elements_course_types ect ON e.id = ect.element_id
            WHERE e.status = 1
              <if test="query.keyword != null and query.keyword != ''">
                AND (
                    e.title LIKE CONCAT('%', #{query.keyword}, '%')
                    OR e.summary LIKE CONCAT('%', #{query.keyword}, '%')
                    OR e.content LIKE CONCAT('%', #{query.keyword}, '%')
                    OR e.keywords LIKE CONCAT('%', #{query.keyword}, '%')
                    OR EXISTS (
                        SELECT 1
                        FROM elements_teaching_courses etc
                        JOIN teaching_courses tc ON tc.id = etc.teaching_course_id
                        WHERE etc.element_id = e.id
                          AND tc.name LIKE CONCAT('%', #{query.keyword}, '%')
                    )
                )
              </if>
              <if test="query.disciplineId != null">
                AND ed.discipline_id = #{query.disciplineId}
              </if>
              <if test="query.elementTypeId != null">
                AND e.element_type_id = #{query.elementTypeId}
              </if>
              <if test="query.courseTypeId != null">
                AND ect.course_type_id = #{query.courseTypeId}
              </if>
              <if test="query.difficulty != null">
                AND e.difficulty = #{query.difficulty}
              </if>
              <if test="query.onlyFavorite != null and query.onlyFavorite">
                AND EXISTS (
                    SELECT 1 FROM element_favorite ef
                    WHERE ef.user_id = #{userId}
                      AND ef.element_id = e.id
                )
              </if>
            </script>
            """)
    Long countPage(SearchQueryDTO query, Integer userId);

    @Select("""
            <script>
            SELECT
                e.id,
                e.title,
                e.summary,
                e.content,
                e.element_type_id,
                et.name AS element_type_name,
                e.difficulty,
                e.keywords,
                e.view_count,
                e.favorite_count,
                e.use_count,
                e.hot_score,
                e.created_at,
                CASE WHEN EXISTS (
                    SELECT 1 FROM element_favorite ef
                    WHERE ef.user_id = #{userId}
                      AND ef.element_id = e.id
                ) THEN 1 ELSE 0 END AS collected_flag,
                CASE WHEN EXISTS (
                    SELECT 1 FROM elements_teacher_types ett
                    WHERE ett.element_id = e.id
                      AND ett.teacher_type = #{teacherType}
                ) THEN 1 ELSE 0 END AS teacher_matched_flag,
                CASE WHEN EXISTS (
                    SELECT 1 FROM elements_disciplines ed2
                    WHERE ed2.element_id = e.id
                      AND ed2.discipline_id = #{userDisciplineId}
                ) THEN 1 ELSE 0 END AS discipline_matched_flag
            FROM elements e
            JOIN element_types et ON et.id = e.element_type_id
            LEFT JOIN elements_disciplines ed ON e.id = ed.element_id
            LEFT JOIN elements_course_types ect ON e.id = ect.element_id
            WHERE e.status = 1
              <if test="query.keyword != null and query.keyword != ''">
                AND (
                    e.title LIKE CONCAT('%', #{query.keyword}, '%')
                    OR e.summary LIKE CONCAT('%', #{query.keyword}, '%')
                    OR e.content LIKE CONCAT('%', #{query.keyword}, '%')
                    OR e.keywords LIKE CONCAT('%', #{query.keyword}, '%')
                    OR EXISTS (
                        SELECT 1
                        FROM elements_teaching_courses etc
                        JOIN teaching_courses tc ON tc.id = etc.teaching_course_id
                        WHERE etc.element_id = e.id
                          AND tc.name LIKE CONCAT('%', #{query.keyword}, '%')
                    )
                )
              </if>
              <if test="query.disciplineId != null">
                AND ed.discipline_id = #{query.disciplineId}
              </if>
              <if test="query.elementTypeId != null">
                AND e.element_type_id = #{query.elementTypeId}
              </if>
              <if test="query.courseTypeId != null">
                AND ect.course_type_id = #{query.courseTypeId}
              </if>
              <if test="query.difficulty != null">
                AND e.difficulty = #{query.difficulty}
              </if>
              <if test="query.onlyFavorite != null and query.onlyFavorite">
                AND EXISTS (
                    SELECT 1 FROM element_favorite ef
                    WHERE ef.user_id = #{userId}
                      AND ef.element_id = e.id
                )
              </if>
            GROUP BY e.id
            ORDER BY
              <choose>
                <when test="query.sortBy == 'popularity'">
                    e.hot_score DESC, e.view_count DESC, e.id DESC
                </when>
                <when test="query.sortBy == 'newest'">
                    e.id DESC
                </when>
                <when test="query.sortBy == 'difficulty'">
                    e.difficulty ASC, e.hot_score DESC, e.id DESC
                </when>
                <otherwise>
                    (
                        CASE
                            WHEN #{query.keyword} IS NOT NULL
                             AND #{query.keyword} != ''
                             AND e.title LIKE CONCAT('%', #{query.keyword}, '%')
                            THEN 30 ELSE 0
                        END
                        +
                        CASE
                            WHEN #{query.keyword} IS NOT NULL
                             AND #{query.keyword} != ''
                             AND e.summary LIKE CONCAT('%', #{query.keyword}, '%')
                            THEN 10 ELSE 0
                        END
                        +
                        CASE
                            WHEN #{query.keyword} IS NOT NULL
                             AND #{query.keyword} != ''
                             AND e.keywords LIKE CONCAT('%', #{query.keyword}, '%')
                            THEN 8 ELSE 0
                        END
                        + e.hot_score
                    ) DESC,
                    e.id DESC
                </otherwise>
              </choose>
            LIMIT #{offset}, #{limit}
            </script>
            """)
    List<ElementRow> pageRows(SearchQueryDTO query, Integer userId, Integer teacherType,
                              Integer userDisciplineId, Integer offset, Integer limit);

    @Select("""
            SELECT
                e.id,
                e.title,
                e.summary,
                e.content,
                e.element_type_id,
                et.name AS element_type_name,
                e.difficulty,
                e.keywords,
                e.view_count,
                e.favorite_count,
                e.use_count,
                e.hot_score,
                e.created_at,
                CASE WHEN EXISTS (
                    SELECT 1 FROM element_favorite ef
                    WHERE ef.user_id = #{userId}
                      AND ef.element_id = e.id
                ) THEN 1 ELSE 0 END AS collected_flag,
                CASE WHEN EXISTS (
                    SELECT 1 FROM elements_teacher_types ett
                    WHERE ett.element_id = e.id
                      AND ett.teacher_type = #{teacherType}
                ) THEN 1 ELSE 0 END AS teacher_matched_flag,
                CASE WHEN EXISTS (
                    SELECT 1 FROM elements_disciplines ed
                    WHERE ed.element_id = e.id
                      AND ed.discipline_id = #{disciplineId}
                ) THEN 1 ELSE 0 END AS discipline_matched_flag
            FROM elements e
            JOIN element_types et ON et.id = e.element_type_id
            WHERE e.status = 1
            ORDER BY e.hot_score DESC, e.favorite_count DESC, e.id DESC
            LIMIT #{limit}
            """)
    List<ElementRow> listRecommendationCandidates(Integer userId, Integer teacherType, Integer disciplineId, Integer limit);

    @Select("""
            SELECT
                e.id,
                e.title,
                e.summary,
                e.content,
                e.element_type_id,
                et.name AS element_type_name,
                e.difficulty,
                e.keywords,
                e.view_count,
                e.favorite_count,
                e.use_count,
                e.hot_score,
                e.created_at,
                CASE WHEN EXISTS (
                    SELECT 1 FROM element_favorite ef
                    WHERE ef.user_id = #{userId}
                      AND ef.element_id = e.id
                ) THEN 1 ELSE 0 END AS collected_flag,
                0 AS teacher_matched_flag,
                0 AS discipline_matched_flag
            FROM elements e
            JOIN element_types et ON et.id = e.element_type_id
            WHERE e.id = #{elementId}
              AND e.status = 1
            LIMIT 1
            """)
    ElementRow selectDetailRow(Integer elementId, Integer userId);

    @Select("""
            SELECT tc.name
            FROM elements_teaching_courses etc
            JOIN teaching_courses tc ON tc.id = etc.teaching_course_id
            WHERE etc.element_id = #{elementId}
              AND tc.status = 1
            ORDER BY tc.id ASC
            """)
    List<String> listTeachingCourseNamesByElementId(Integer elementId);

    @Select("""
            SELECT d.name
            FROM elements_disciplines ed
            JOIN disciplines d ON d.id = ed.discipline_id
            WHERE ed.element_id = #{elementId}
              AND d.status = 1
            ORDER BY d.id ASC
            """)
    List<String> listDisciplineNamesByElementId(Integer elementId);

    @Select("""
            SELECT ct.name
            FROM elements_course_types ect
            JOIN course_types ct ON ct.id = ect.course_type_id
            WHERE ect.element_id = #{elementId}
              AND ct.status = 1
            ORDER BY ct.id ASC
            """)
    List<String> listCourseTypeNamesByElementId(Integer elementId);

    @Update("UPDATE elements SET view_count = view_count + 1, updated_at = NOW() WHERE id = #{elementId}")
    int increaseViewCount(Integer elementId);

    @Update("""
            UPDATE elements
            SET favorite_count = favorite_count + 1,
                updated_at = NOW()
            WHERE id = #{elementId}
            """)
    int increaseFavoriteCount(Integer elementId);

    @Update("""
            UPDATE elements
            SET favorite_count = CASE WHEN favorite_count > 0 THEN favorite_count - 1 ELSE 0 END,
                updated_at = NOW()
            WHERE id = #{elementId}
            """)
    int decreaseFavoriteCount(Integer elementId);
}
