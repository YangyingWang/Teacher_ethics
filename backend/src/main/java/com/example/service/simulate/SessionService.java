package com.example.service.simulate;

import com.example.mapper.simulate.chat.ChatMessageMapper;
import com.example.mapper.simulate.chat.ChatSessionMapper;
import com.example.pojo.simulate.chat.ChatSession;
import com.example.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class SessionService {
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    ChatMessageMapper messageMapper;

    @Transactional
    public ChatSession createSession(String title, Integer maxSteps, Integer sceneCategoryId) {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");

        ChatSession s = new ChatSession();
        s.setUserId(userId);
        s.setSceneCategoryId(sceneCategoryId);
        s.setTitle(title == null || title.isBlank() ? "新决策训练" : title);
        s.setStatus("active");
        s.setPhase("init");
        s.setStep(0);
        s.setMaxSteps(maxSteps);
        sessionMapper.insert(s);

        return sessionMapper.selectById(s.getId());
    }

    @Transactional
    public boolean deleteSession(Integer sessionId) {
        ChatSession session = sessionMapper.selectById(sessionId);
        if (session == null) return false;

        // 手动先删 messages（即使你建了 CASCADE，这一步也不会有问题，只是多一次SQL）
        messageMapper.deleteBySessionId(sessionId);
        sessionMapper.deleteById(sessionId);
        return true;
    }

    public List<ChatSession> listSessions() {
        Map<String,Object> map= ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        return sessionMapper.listByUserId(userId);
    }

    public ChatSession getSession(Integer id) {
        return sessionMapper.selectById(id);
    }

    public void updateTitle(Integer id, String title) {
        sessionMapper.updateTitle(id,title);
    }

    public void updateMaxSteps(Integer id, Integer maxSteps) {
        sessionMapper.updateMaxSteps(id,maxSteps);
    }
}