package com.example.mapper.simulate.chat;

import com.example.pojo.simulate.chat.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    @Insert("insert into chat_sessions(user_id, scene_category_id, title, status, phase, step, max_steps, created_at, updated_at)" +
            " values (#{userId}, #{sceneCategoryId}, #{title}, #{status}, #{phase}, #{step}, #{maxSteps}, NOW(), NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(ChatSession session);

    @Delete("delete from chat_sessions where id = #{id}")
    void deleteById(Integer id);

    @Select("select * from chat_sessions where id = #{id}")
    ChatSession selectById(Integer id);

    @Select("select * from chat_sessions where user_id = #{userId} ORDER BY updated_at DESC")
    List<ChatSession> listByUserId(Integer userId);

    @Update("update chat_sessions set status=#{status},phase=#{phase}, updated_at = NOW() where id=#{id}")
    void updateComplete(Integer id, String status, String phase);

    @Update("update chat_sessions set title=#{title},updated_at = NOW() where id=#{id}")
    void updateTitle(Integer id, String title);

    @Update("update chat_sessions set max_steps=#{maxSteps},updated_at = NOW() where id=#{id}")
    void updateMaxSteps(Integer id, Integer maxSteps);

    @Update("update chat_sessions set phase=#{phase},step=#{step},updated_at = NOW() where id = #{id}")
    void updatePhase(Integer id, String phase, Integer step);

    @Update("update chat_sessions set scenario=#{scenario}, updated_at = now() where id = #{id} ")
    int updateScenario(Integer id, String scenario);
}