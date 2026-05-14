package com.example.service.simulate;

import com.example.mapper.simulate.chat.ChatMessageMapper;
import com.example.pojo.simulate.chat.ChatMessage;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MessageService {
    @Autowired
    private ChatMessageMapper messageMapper;

    public List<ChatMessage> getMessagesBySessionId(Integer sessionId) {
        return messageMapper.listBySessionId(sessionId);
    }
}
