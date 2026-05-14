package com.example.mapper.study.course;

import com.example.dto.study.course.CourseHomeDTO;
import com.example.pojo.study.course.CourseLearning;
import com.example.pojo.study.course.CourseUserLearn;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface CourseLearningMapper {

    @Select("SELECT * FROM course_learning WHERE user_id = #{userId} AND course_id = #{courseId}")
    CourseLearning selectByUserIdAndCourseId(Integer userId, Integer courseId);

    @Insert("""
            INSERT INTO course_learning(
                user_id, course_id, status, progress_percent, last_sec, study_total,
                last_time, started_at, completed_at, created_at, updated_at
            ) VALUES (
                #{userId}, #{courseId}, #{status}, #{progressPercent}, #{lastSec}, #{studyTotal},
                #{lastTime}, #{startedAt}, #{completedAt}, NOW(), NOW()
            )
            """)
    int insert(CourseLearning learning);

    @Update("""
            UPDATE course_learning
            SET status = #{status},
                progress_percent = #{progressPercent},
                last_sec = #{lastSec},
                study_total = #{studyTotal},
                last_time = #{lastTime},
                started_at = #{startedAt},
                completed_at = #{completedAt},
                updated_at = NOW()
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int update(CourseLearning learning);

    @Update("""
            UPDATE course_learning
            SET status = 1,
                progress_percent = 0,
                last_sec = 0,
                study_total = 0,
                last_time = NOW(),
                started_at = NOW(),
                completed_at = NULL,
                updated_at = NOW()
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
            """)
    int resetByUserIdAndCourseId(Integer userId, Integer courseId);

    @Delete("DELETE FROM course_learning WHERE user_id = #{userId} AND course_id = #{courseId}")
    int deleteByUserIdAndCourseId(Integer userId, Integer courseId);

    @Options(useGeneratedKeys = true, keyProperty = "id")
    @Insert("""
            INSERT INTO course_user_learn(
                user_id, course_id, learn_date, start_time, end_time,
                study_sec, progress_before, progress_after, created_at
            ) VALUES (
                #{userId}, #{courseId}, #{learnDate}, #{startTime}, #{endTime},
                #{studySec}, #{progressBefore}, #{progressAfter}, NOW()
            )
            """)
    int insertLearnRecord(CourseUserLearn record);

    @Select("""
            SELECT *
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND course_id = #{courseId}
              AND start_time = #{startTime}
            LIMIT 1
            """)
    CourseUserLearn selectLearnRecordBySession(Integer userId, Integer courseId, LocalDateTime startTime);

    @Update("""
            UPDATE course_user_learn
            SET end_time = #{endTime},
                study_sec = #{studySec},
                progress_after = #{progressAfter}
            WHERE id = #{id}
            """)
    int updateLearnRecord(CourseUserLearn record);

    @Select("""
            SELECT
                c.id AS id,
                c.title AS title,
                c.cover_url AS cover,
                c.video_url AS videoUrl,
                c.category_id AS categoryId,
                cc.name AS categoryName,
                c.duration AS duration,
                ROUND(COALESCE(cl.progress_percent, 0)) AS progress,
                CASE WHEN cf.id IS NULL THEN FALSE ELSE TRUE END AS favorite,
                cl.last_time AS lastStudyTime
            FROM course_learning cl
            JOIN courses c ON c.id = cl.course_id
            LEFT JOIN course_category cc ON cc.id = c.category_id
            LEFT JOIN course_favorite cf ON cf.course_id = c.id AND cf.user_id = #{userId}
            WHERE cl.user_id = #{userId}
              AND (#{status} IS NULL OR cl.status = #{status})
            ORDER BY
                CASE cl.status WHEN 1 THEN 0 WHEN 0 THEN 1 ELSE 2 END,
                cl.last_time DESC,
                cl.updated_at DESC
            """)
    List<CourseHomeDTO.MyCourseItem> listMyCourses(Integer userId, Integer status);

    @Select("""
            SELECT
                cul.id AS id,
                cul.course_id AS courseId,
                c.title AS courseTitle,
                CASE
                   WHEN COALESCE(cul.study_sec, 0) <= 0 THEN 0
                   WHEN cul.study_sec < 60 THEN 1
                   ELSE CEIL(cul.study_sec / 60.0)
               END AS duration,
                COALESCE(cul.end_time, cul.start_time, cul.created_at) AS timestamp,
                cul.progress_before AS progressBefore,
                cul.progress_after AS progressAfter
            FROM course_user_learn cul
            JOIN courses c ON c.id = cul.course_id
            WHERE cul.user_id = #{userId}
            ORDER BY COALESCE(cul.end_time, cul.start_time, cul.created_at) DESC
            LIMIT #{limit}
            """)
    List<CourseHomeDTO.RecordItem> listLearningRecords(Integer userId, Integer limit);

    @Select("SELECT COUNT(*) FROM course_learning WHERE user_id = #{userId}")
    Integer countTotalCourses(Integer userId);

    @Select("SELECT COUNT(*) FROM course_learning WHERE user_id = #{userId} AND status = 2")
    Integer countCompletedCourses(Integer userId);

    @Select("SELECT COUNT(*) FROM course_learning WHERE user_id = #{userId} AND status = 1")
    Integer countInProgressCourses(Integer userId);

    @Select("SELECT COUNT(*) FROM course_learning WHERE user_id = #{userId} AND status = 0")
    Integer countNotStartedCourses(Integer userId);

    @Select("SELECT COALESCE(SUM(study_sec), 0) FROM course_user_learn WHERE user_id = #{userId}")
    Integer sumStudySeconds(Integer userId);

    @Select("SELECT ROUND(AVG(progress_percent)) FROM course_learning WHERE user_id = #{userId}")
    Integer avgProgressPercent(Integer userId);

    @Select("""
            SELECT ROUND(AVG(study_sec))
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer avgSessionStudySeconds(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("SELECT DISTINCT learn_date FROM course_user_learn WHERE user_id = #{userId} ORDER BY learn_date DESC")
    List<LocalDate> listActiveDates(Integer userId);

    @Select("""
        SELECT
            learn_date AS date,
            ROUND(COALESCE(SUM(study_sec), 0) / 3600.0, 1) AS studyHours
        FROM course_user_learn
        WHERE user_id = #{userId}
          AND learn_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY learn_date
        ORDER BY learn_date ASC
        """)
    List<CourseHomeDTO.CalendarDay> listCalendarDays(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM course_learning
            WHERE user_id = #{userId}
              AND status = 2
              AND completed_at IS NOT NULL
              AND DATE(completed_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countCompletedCoursesInRange(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
        SELECT
            cc.name AS label,
            ROUND(COALESCE(SUM(cul.study_sec), 0) / 60.0, 1) AS value
        FROM course_user_learn cul
        JOIN courses c ON c.id = cul.course_id
        LEFT JOIN course_category cc ON cc.id = c.category_id
        WHERE cul.user_id = #{userId}
          AND cul.learn_date BETWEEN #{startDate} AND #{endDate}
        GROUP BY cc.id, cc.name
        HAVING SUM(cul.study_sec) > 0
        ORDER BY SUM(cul.study_sec) DESC, cc.id ASC
        """)
    List<CourseHomeDTO.ChartItem> listDurationDistribution(Integer userId, LocalDate startDate, LocalDate endDate);
    @Select("""
            SELECT
                CASE
                    WHEN HOUR(start_time) BETWEEN 0 AND 5 THEN '凌晨'
                    WHEN HOUR(start_time) BETWEEN 6 AND 11 THEN '上午'
                    WHEN HOUR(start_time) BETWEEN 12 AND 17 THEN '下午'
                    ELSE '晚上'
                END AS label,
                COUNT(*) AS value
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
              AND start_time IS NOT NULL
            GROUP BY label
            ORDER BY CASE label
                WHEN '凌晨' THEN 1
                WHEN '上午' THEN 2
                WHEN '下午' THEN 3
                ELSE 4
            END
            """)
    List<CourseHomeDTO.ChartItem> listTimeDistribution(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(study_sec), 0)
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer sumStudySecondsInRange(Integer userId, LocalDate startDate, LocalDate endDate);

}
