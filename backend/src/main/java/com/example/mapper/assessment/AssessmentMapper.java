package com.example.mapper.assessment;

import com.example.pojo.Assessment;
import org.apache.ibatis.annotations.*;

import java.time.LocalDate;
import java.util.List;

@Mapper
public interface AssessmentMapper {
    @Insert(""" 
            INSERT INTO assessments(
                user_id, record_date, overall_score, overall_level,learning_score, ability_score, governance_score,
                monthly_change, last_days, total_activities,improvement_rate, ranking, created_at, updated_at
            ) VALUES (
                #{userId}, #{recordDate}, #{overallScore}, #{overallLevel},#{learningScore}, #{abilityScore}, #{governanceScore},
                #{monthlyChange}, #{lastDays}, #{totalActivities},#{improvementRate}, #{ranking}, NOW(), NOW()
            )
            ON DUPLICATE KEY UPDATE
                overall_score = VALUES(overall_score),
                overall_level = VALUES(overall_level),
                learning_score = VALUES(learning_score),
                ability_score = VALUES(ability_score),
                governance_score = VALUES(governance_score),
                monthly_change = VALUES(monthly_change),
                last_days = VALUES(last_days),
                total_activities = VALUES(total_activities),
                improvement_rate = VALUES(improvement_rate),
                ranking = VALUES(ranking),
                updated_at = NOW()
            """)
    int upsert(Assessment assessment);

    @Select("SELECT * FROM assessments WHERE user_id = #{userId} AND record_date = #{recordDate}")
    Assessment selectByUserIdAndDate(Integer userId, LocalDate recordDate);

    @Select("SELECT * FROM assessments WHERE user_id = #{userId} ORDER BY record_date DESC LIMIT 1")
    Assessment selectLatestByUserId(Integer userId);

    @Select("""
            SELECT * FROM assessments
            WHERE user_id = #{userId}
              AND record_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY record_date DESC
            LIMIT 1
            """)
    Assessment selectLatestByUserIdAndRange(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT * FROM assessments
            WHERE user_id = #{userId}
              AND record_date < #{recordDate}
            ORDER BY record_date DESC
            LIMIT 1
            """)
    Assessment selectPreviousByUserId(Integer userId, LocalDate recordDate);

    @Select("""
            SELECT * FROM assessments
            WHERE user_id = #{userId}
              AND record_date BETWEEN #{startDate} AND #{endDate}
            ORDER BY record_date ASC
            """)
    List<Assessment> listByUserIdAndRange(Integer userId, LocalDate startDate, LocalDate endDate);

    @Select("SELECT COUNT(*) FROM assessments WHERE record_date = #{recordDate}")
    Integer countByRecordDate(LocalDate recordDate);

    @Select("""
            SELECT COUNT(*)
            FROM assessments
            WHERE record_date = #{recordDate}
              AND overall_score > #{overallScore}
            """)
    Integer countBetterByRecordDate(LocalDate recordDate, Integer overallScore);

    @Select("""
            SELECT ROUND(AVG(learning_score))
            FROM assessments
            WHERE record_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer avgLearningScore(LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(ability_score))
            FROM assessments
            WHERE record_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer avgAbilityScore(LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(governance_score))
            FROM assessments
            WHERE record_date BETWEEN #{startDate} AND #{endDate}
            """)
    Integer avgGovernanceScore(LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(learning_score))
            FROM assessments
            WHERE record_date BETWEEN #{startDate} AND #{endDate}
              AND overall_score >= 90
            """)
    Integer avgLearningScoreExcellent(LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(ability_score))
            FROM assessments
            WHERE record_date BETWEEN #{startDate} AND #{endDate}
              AND overall_score >= 90
            """)
    Integer avgAbilityScoreExcellent(LocalDate startDate, LocalDate endDate);

    @Select("""
            SELECT ROUND(AVG(governance_score))
            FROM assessments
            WHERE record_date BETWEEN #{startDate} AND #{endDate}
              AND overall_score >= 90
            """)
    Integer avgGovernanceScoreExcellent(LocalDate startDate, LocalDate endDate);
}
