package com.example.mapper;

import com.example.pojo.Admin;
import com.example.vo.*;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface AdminMapper {
    @Select("select id,username,password,real_name,status,created_at,updated_at from admins where username=#{username}")
    Admin findByUsername(String username);

//    主页数据汇总展示
    @Select("SELECT " +
            "(SELECT COUNT(*) FROM users) AS teacherTotal, " +
            "(SELECT COUNT(DISTINCT user_id) FROM course_learning) AS courseLearnerCount, " +
            "(SELECT COUNT(DISTINCT user_id) FROM chat_sessions) AS simulationParticipantCount, " +
            "(SELECT COUNT(*) FROM evaluations) AS evaluationCount, " +
            "(SELECT COUNT(*) FROM assessments) AS assessmentCount, " +
            "COALESCE((SELECT ROUND(AVG(overall_score), 1) FROM assessments), 0) AS avgAssessmentScore")
    AdminSummaryVO getSummary();

    @Select("SELECT " +
            "CASE type " +
            "WHEN 0 THEN '教学型' " +
            "WHEN 1 THEN '科研型' " +
            "WHEN 2 THEN '综合型' " +
            "ELSE '未分类' END AS name, " +
            "COUNT(*) AS value " +
            "FROM users GROUP BY type ORDER BY type")
    List<NameValueVO> getTeacherTypeStats();

    @Select("SELECT " +
            "COALESCE(d.name, '未分配院系') AS name, COUNT(*) AS value " +
            "FROM users u LEFT JOIN departments d ON u.dep_id = d.id " +
            "GROUP BY u.dep_id, d.name ORDER BY value DESC")
    List<NameValueVO> getDepartmentStats();

    @Select("SELECT '学习筑基' AS name, COUNT(DISTINCT user_id) AS value FROM course_learning " +
            "UNION ALL " +
            "SELECT '治理研修' AS name, COUNT(DISTINCT user_id) AS value FROM chat_sessions " +
            "UNION ALL " +
            "SELECT '决策评估' AS name, COUNT(*) AS value FROM evaluations " +
            "UNION ALL " +
            "SELECT '多维评估' AS name, COUNT(*) AS value FROM assessments")
    List<NameValueVO> getModuleStats();

    @Select("SELECT * FROM (" +
            "SELECT '教师注册' AS title, CONCAT('教师账号 ', username, ' 完成注册') AS content, DATE_FORMAT(created_at, '%Y-%m-%d %H:%i:%s') AS time, 'user' AS type, created_at AS sort_time FROM users " +
            "UNION ALL " +
            "SELECT '治理研修会话' AS title, CONCAT('教师 ', u.username, ' 新建了治理研修会话') AS content, DATE_FORMAT(s.created_at, '%Y-%m-%d %H:%i:%s') AS time, 'simulate' AS type, s.created_at AS sort_time FROM chat_sessions s LEFT JOIN users u ON s.user_id = u.id " +
            "UNION ALL " +
            "SELECT '多维评估生成' AS title, CONCAT('教师 ', u.username, ' 生成了多维评估记录') AS content, DATE_FORMAT(a.created_at, '%Y-%m-%d %H:%i:%s') AS time, 'assessment' AS type, a.created_at AS sort_time FROM assessments a LEFT JOIN users u ON a.user_id = u.id " +
            ") t ORDER BY sort_time DESC LIMIT 5")
    List<RecentActivityVO> getRecentActivities();


//    教师管理
    int countTeachers(@Param("username") String username, @Param("realName") String realName,
                       @Param("type") Integer type, @Param("depId") Integer depId);

    List<AdminTeacherPageVO> pageTeachers(@Param("username") String username, @Param("realName") String realName,
                                          @Param("type") Integer type, @Param("depId") Integer depId,
                                          @Param("offset") Integer offset, @Param("pageSize") Integer pageSize);

    AdminTeacherDetailVO getTeacherDetail(@Param("id") Integer id);

    @Select("SELECT id AS value, name FROM departments ORDER BY id ASC")
    List<NameValueVO> listDepartments();


//    数据分析比对
    AdminStatisticsSummaryVO getStatisticsSummary(@Param("type") Integer type, @Param("depId") Integer depId);

    List<NameValueVO> getTeacherTypeStatsByDepId(@Param("depId") Integer depId);

    List<NameValueVO> getDepartmentStatsByType(@Param("type") Integer type);

    List<NameValueVO> getLevelStats(@Param("type") Integer type, @Param("depId") Integer depId);

    List<NameValueVO> getAssessmentTrend(@Param("type") Integer type, @Param("depId") Integer depId);

    List<NameValueVO> getRegisterTrend(@Param("type") Integer type, @Param("depId") Integer depId);

    List<NameValueVO> getSimulationStats(@Param("type") Integer type, @Param("depId") Integer depId);
}
