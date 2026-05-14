package com.example.mapper.assessment;

import com.example.dto.assessment.AssessmentAdviceRow;
import com.example.dto.assessment.AssessmentDimensionRow;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AssessmentAnalyticsMapper {

    /**
     * 学习模块统计
     * 1. course_user_learn：单次学习会话/行为记录
     * 2. course_learning：课程整体学习状态
     */
    @Select("""
            SELECT COUNT(*)
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countLearningActivities(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COALESCE(SUM(study_sec), 0)
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer sumLearningStudySeconds(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COUNT(DISTINCT learn_date)
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countLearningActiveDays(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COUNT(DISTINCT course_id)
            FROM course_user_learn
            WHERE user_id = #{userId}
              AND learn_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countParticipatedLearningCourses(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT COUNT(*)
            FROM course_learning
            WHERE user_id = #{userId}
              AND status = 2
              AND completed_at IS NOT NULL
              AND DATE(completed_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countCompletedLearningCourses(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(COALESCE(AVG(cl.progress_percent), 0))
            FROM course_learning cl
            JOIN (
                SELECT DISTINCT course_id
                FROM course_user_learn
                WHERE user_id = #{userId}
                  AND learn_date BETWEEN #{startDate} AND #{endDate}
            ) t ON t.course_id = cl.course_id
            WHERE cl.user_id = #{userId}
            """)
    Integer avgLearningProgress(Integer userId, LocalDate startDate, LocalDate endDate);

    /**
     * 能力提升（情景测试）统计，保留原有逻辑
     */
    @Select("""
            SELECT COUNT(*)
            FROM questionnaires
            WHERE user_id = #{userId}
              AND status = 1
              AND DATE(submitted_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countAbilityActivities(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(
                CASE
                    WHEN total_score IS NULL OR total_score = 0 THEN 0
                    ELSE user_total_score * 100.0 / total_score
                END
            ))
            FROM questionnaires
            WHERE user_id = #{userId}
              AND status = 1
              AND DATE(submitted_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer avgAbilityScore(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT MAX(
                CASE
                    WHEN total_score IS NULL OR total_score = 0 THEN 0
                    ELSE ROUND(user_total_score * 100.0 / total_score)
                END
            )
            FROM questionnaires
            WHERE user_id = #{userId}
              AND status = 1
              AND DATE(submitted_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer maxAbilityScore(Integer userId, LocalDate startDate, LocalDate endDate);

    /**
     * 治理研修统计，保留原有逻辑
     */
    @Select("""
            SELECT COUNT(*)
            FROM evaluations
            WHERE user_id = #{userId}
              AND status = 1
              AND DATE(created_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer countGovernanceActivities(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(overall_score))
            FROM evaluations
            WHERE user_id = #{userId}
              AND status = 1
              AND DATE(created_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer avgGovernanceScore(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(
                CASE
                    WHEN COUNT(*) = 0 THEN 0
                    ELSE SUM(CASE WHEN risk_level = 1 THEN 1 ELSE 0 END) * 100.0 / COUNT(*)
                END
            )
            FROM evaluations
            WHERE user_id = #{userId}
              AND status = 1
              AND DATE(created_at) BETWEEN #{startDate} AND #{endDate}
            """)
    Integer governanceHighRiskRate(Integer userId, LocalDate startDate, LocalDate endDate);

    /**
     * 个人维度：情景测试(report) + 治理研修(evaluation) 合并后按名称聚合
     */
    @Select("""
            SELECT t.name AS name, ROUND(AVG(t.score)) AS score
            FROM (
                SELECT rd.name AS name, rds.score AS score
                FROM report_dimension_scores rds
                JOIN reports r ON r.id = rds.report_id
                JOIN report_dimensions rd ON rd.id = rds.dimension_id
                WHERE r.user_id = #{userId}
                  AND DATE(r.created_at) BETWEEN #{startDate} AND #{endDate}

                UNION ALL

                SELECT ed.name AS name, eds.score AS score
                FROM evaluation_dimension_scores eds
                JOIN evaluations e ON e.id = eds.evaluation_id
                JOIN evaluation_dimensions ed ON ed.id = eds.dimension_id
                WHERE e.user_id = #{userId}
                  AND e.status = 1
                  AND DATE(e.created_at) BETWEEN #{startDate} AND #{endDate}
            ) t
            GROUP BY t.name
            ORDER BY score DESC, name ASC
            """)
    List<AssessmentDimensionRow> listPersonalDimensions(Integer userId, LocalDate startDate, LocalDate endDate);

    /**
     * 对比维度：department 暂时回退为 platform；
     * excellent 使用高分群体（report/evaluation 总分 >= 90）
     */
    @Select("""
            SELECT t.name AS name, ROUND(AVG(t.score)) AS score
            FROM (
                SELECT rd.name AS name, rds.score AS score
                FROM report_dimension_scores rds
                JOIN reports r ON r.id = rds.report_id
                JOIN report_dimensions rd ON rd.id = rds.dimension_id
                WHERE DATE(r.created_at) BETWEEN #{startDate} AND #{endDate}
                  AND (
                    #{comparisonTarget} IN ('department', 'university')
                    OR (#{comparisonTarget} = 'excellent' AND r.overall_score >= 90)
                  )

                UNION ALL

                SELECT ed.name AS name, eds.score AS score
                FROM evaluation_dimension_scores eds
                JOIN evaluations e ON e.id = eds.evaluation_id
                JOIN evaluation_dimensions ed ON ed.id = eds.dimension_id
                WHERE e.status = 1
                  AND DATE(e.created_at) BETWEEN #{startDate} AND #{endDate}
                  AND (
                    #{comparisonTarget} IN ('department', 'university')
                    OR (#{comparisonTarget} = 'excellent' AND e.overall_score >= 90)
                  )
            ) t
            GROUP BY t.name
            ORDER BY score DESC, name ASC
            """)
    List<AssessmentDimensionRow> listCompareDimensions(String comparisonTarget, LocalDate startDate, LocalDate endDate);

    /**
     * AI 建议：从情景测试建议 + 决策评估建议中抽取最近若干条
     */
    @Select("""
            SELECT *
            FROM (
                SELECT
                    rs.priority AS priority,
                    rs.title AS title,
                    rs.content AS description,
                    rs.created_at AS created_at
                FROM report_suggestions rs
                JOIN reports r ON r.id = rs.report_id
                WHERE r.user_id = #{userId}

                UNION ALL

                SELECT
                    es.priority AS priority,
                    es.title AS title,
                    es.content AS description,
                    es.created_at AS created_at
                FROM evaluation_suggestions es
                JOIN evaluations e ON e.id = es.evaluation_id
                WHERE e.user_id = #{userId}
                  AND e.status = 1
            ) t
            ORDER BY priority ASC, created_at DESC
            LIMIT #{limit}
            """)
    List<AssessmentAdviceRow> listLatestAdvice(Integer userId, Integer limit);
}