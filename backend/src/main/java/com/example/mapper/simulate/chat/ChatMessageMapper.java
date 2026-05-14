package com.example.mapper.simulate.chat;

import com.example.pojo.simulate.chat.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    @Insert("INSERT INTO chat_messages (session_id, role, content, content_format, seq, temperature, created_at) " +
            "VALUES (#{sessionId}, #{role}, #{content}, #{contentFormat}, #{seq}, #{temperature, jdbcType=DECIMAL}, NOW())")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    void insert(ChatMessage message);

    @Delete("delete from chat_messages where session_id = #{sessionId}")
    void deleteBySessionId(Integer sessionId);

    @Select("select COALESCE(MAX(seq), 0) from chat_messages where session_id = #{sessionId}")
    Integer selectMaxSeq(Integer sessionId);

    @Select("select * from chat_messages where session_id=#{sessionId} order by seq DESC LIMIT #{limit}")
    List<ChatMessage> listRecent(Integer sessionId, int limit);

    @Select("select * from chat_messages where session_id=#{sessionId} order by seq ASC")
    List<ChatMessage> listBySessionId(Integer sessionId);

    @Select("select * from chat_messages where id=#{id}")
    ChatMessage selectById(Integer id);
}




