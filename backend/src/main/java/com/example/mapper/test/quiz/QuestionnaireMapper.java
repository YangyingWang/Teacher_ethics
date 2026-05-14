package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.Questionnaire;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface QuestionnaireMapper {
    @Insert("INSERT INTO questionnaires(user_id, scene_id, title, question_sequence, total_score, total_count, user_total_score, status, started_at, time_spent, created_at) " +
            "VALUES(#{userId}, #{sceneId}, #{title}, #{questionSequence}, #{totalScore}, #{totalCount}, #{userTotalScore}, #{status}, #{startedAt}, #{timeSpent}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(Questionnaire qn);

    @Delete("DELETE FROM questionnaires WHERE id=#{id}")
    int deleteById(Integer id);

    @Select("SELECT * FROM questionnaires WHERE id = #{id}")
    Questionnaire selectById(Integer id);

    @Update("UPDATE questionnaires SET user_total_score = #{userTotalScore}, status = #{status}, " +
            "submitted_at = #{submittedAt}, time_spent = #{timeSpent}, updated_at = NOW() " +
            "WHERE id = #{id}")
    int update(Questionnaire qn);

    @Select("SELECT * FROM questionnaires WHERE user_id = #{userId} AND status = 1 " +
            "ORDER BY submitted_at DESC LIMIT 3")
    List<Questionnaire> selectRecentByUserId(Integer userId);

    @Select("SELECT * FROM questionnaires WHERE user_id = #{userId} AND status = 1 ")
    List<Questionnaire> selectByUserId(Integer userId);
}
