package com.example.service.simulate;

import com.example.dto.simulate.ChatReply;
import com.example.dto.simulate.ChatRequest;
import com.example.mapper.simulate.chat.ChatMessageMapper;
import com.example.mapper.simulate.chat.ChatSessionMapper;
import com.example.pojo.simulate.chat.ChatMessage;
import com.example.pojo.simulate.chat.ChatSession;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Service
public class ChatService {
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private ChatModel chatModel; // Spring AI
    private static final int MEMORY_LIMIT = 20; // 给模型的上下文窗口条数

    @Transactional
    public ChatReply chat(ChatRequest req) {
        if (req.getSessionId() == null) throw new IllegalArgumentException("sessionId不能为空");
        ChatSession session = sessionMapper.selectById(req.getSessionId());
        if (session == null) throw new IllegalArgumentException("该会话不存在！");

        String userText = req.getMessage() == null ? "" : req.getMessage().trim();
        if (userText.isEmpty()) throw new IllegalArgumentException("message不能为空");

        String currentPhase = session.getPhase();
        return switch (currentPhase) {
            case "init" -> handleInit(session, userText);
            case "dialogue" -> handleDialogue(session, userText);
            case "evaluating" -> throw new IllegalStateException("当前会话正在生成评估，请勿继续对话");
            case "completed" -> throw new IllegalStateException("当前会话已完成，请新建会话开始新的演练");
            default -> throw new IllegalStateException("未知会话阶段：" + currentPhase);
        };
    }

    /**
     * init阶段：用户第一次输入的是“情景主题”，系统据此生成具体情景，然后直接把 phase 切到 dialogue。
     */
    private ChatReply handleInit(ChatSession session, String topicText) {
        sessionMapper.updateTitle(session.getId(), topicText);

        int nextSeqUser = nextSeq(session.getId());
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("user");
        userMsg.setContent(topicText);
        userMsg.setContentFormat("markdown");
        userMsg.setSeq(nextSeqUser);
        messageMapper.insert(userMsg);

        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(buildInitSystemPrompt()));
        promptMessages.add(new UserMessage(topicText));

        Prompt prompt = new Prompt(promptMessages);
        String scenarioText = chatModel.call(prompt).getResult().getOutput().getText();

        int nextSeqAi = nextSeqUser + 1;
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(session.getId());
        aiMsg.setRole("assistant");
        aiMsg.setContent(scenarioText);
        aiMsg.setContentFormat("markdown");
        aiMsg.setSeq(nextSeqAi);
        aiMsg.setTemperature(new BigDecimal("0.700"));
        messageMapper.insert(aiMsg);

        // 保存本次会话真正使用的情景，并切换到 dialogue
        sessionMapper.updateScenario(session.getId(), scenarioText);
        sessionMapper.updatePhase(session.getId(), "dialogue", 0);

        return ChatReply.builder()
                .sessionId(session.getId())
                .aiSeq(nextSeqAi)
                .aiMessage(scenarioText)
                .phase("dialogue")
                .step(0)
                .build();
    }

    /**
     * dialogue阶段：每次用户输入一个决策，系统推演一次后果。
     * step + 1，达到 maxSteps 后 phase 切到 evaluating。
     */
    private ChatReply handleDialogue(ChatSession session, String userText) {
        int currentStep = session.getStep();
        int maxSteps = session.getMaxSteps();

        int nextSeqUser = nextSeq(session.getId());
        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setRole("user");
        userMsg.setContent(userText);
        userMsg.setContentFormat("markdown");
        userMsg.setSeq(nextSeqUser);
        messageMapper.insert(userMsg);

        List<ChatMessage> recent = messageMapper.listRecent(session.getId(), MEMORY_LIMIT);
        Collections.reverse(recent);

        List<Message> promptMessages = new ArrayList<>();
        promptMessages.add(new SystemMessage(buildDialogueSystemPrompt(session, currentStep, maxSteps)));
        for (ChatMessage m : recent) {
            promptMessages.add(toSpringAiMessage(m));
        }

        Prompt prompt = new Prompt(promptMessages);
        String assistantText = chatModel.call(prompt).getResult().getOutput().getText();

        int nextSeqAi = nextSeqUser + 1;
        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(session.getId());
        aiMsg.setRole("assistant");
        aiMsg.setContent(assistantText);
        aiMsg.setContentFormat("markdown");
        aiMsg.setSeq(nextSeqAi);
        messageMapper.insert(aiMsg);

        int newStep = currentStep + 1;
        String newPhase = (newStep >= maxSteps) ? "evaluating" : "dialogue";
        sessionMapper.updatePhase(session.getId(), newPhase, newStep);

        return ChatReply.builder()
                .sessionId(session.getId())
                .aiSeq(nextSeqAi)
                .aiMessage(assistantText)
                .phase(newPhase)
                .step(newStep)
                .build();
    }

    private int nextSeq(Integer sessionId) {
        Integer maxSeq = messageMapper.selectMaxSeq(sessionId);
        return (maxSeq == null ? 0 : maxSeq) + 1;
    }

    private Message toSpringAiMessage(ChatMessage m) {
        String c = m.getContent() == null ? "" : m.getContent();
        return switch (m.getRole()) {
            case "user" -> new UserMessage(c);
            case "system" -> new SystemMessage(c);
            default -> new AssistantMessage(c);
        };
    }

    private String buildInitSystemPrompt() {
        return """
                你是高校教师师德师风治理研修系统中的情景生成助手.
                请基于最新的师德规范和政策，为用户输入的主题生成一个逼真的详细案例情景描述。

                输出要求：
                1. 只输出“具体情景描述”，不要输出分析、建议、步骤、应对原则、方法论、清单、指南等。
                2. 必须包含：时间、地点、至少3个相关角色（教师/学生/同事或领导等）、事件起因、发展过程、当前困境。
                3. 必须体现明显的师德/伦理冲突。
                4. 可以自然融入相关规范依据，但不要展开成条文讲解。
                5. 字数控制在300-500字。
                6. 语言客观、中立、真实，像一个正在发生的高校管理案例。
                7. 用叙事文本，不用“第一/第二/第三”这种培训讲义结构。
                """;
    }

    private String buildDialogueSystemPrompt(ChatSession session, int currentStep, int maxSteps) {
        String scenario = session.getScenario() == null ? "" : session.getScenario();
        int currentRound = currentStep + 1;

        return String.format("""
                你正在高校教师师德师风治理研修系统中扮演“沙盘推演引擎”。

                本次固定情景如下：
                %s

                任务要求：
                1. 用户现在扮演事件处置者，每次输入的是一项处置决策或应对意见。
                2. 你需要根据用户的决策，推演事件的发展变化。
                3. 你要模拟不同相关方的反应，例如学生、同事、学院领导、舆情、家长等。
                4. 你要展示该决策可能带来的短期影响和后续风险。
                5. 你可以引入新的变化，但必须与原始情景一致，不能脱离场景胡乱展开。
                6. 你不要直接给标准答案，也不要提前进入总结评估。
                7. 你的回复要服务于“下一轮继续决策”，因此结尾最好自然留下新的待处理问题。

                当前是第 %d / %d 轮推演。
                请继续输出本轮推演结果。
                """, scenario, currentRound, maxSteps);
    }
}
