package com.example.service.test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.deepseek.DeepSeekChatModel;
import org.springframework.ai.deepseek.DeepSeekChatOptions;
import org.springframework.ai.deepseek.api.ResponseFormat;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Slf4j
@Service
public class AIApiService {
    @Autowired
    private DeepSeekChatModel chatModel;
    @Autowired
    private ObjectMapper objectMapper;

    public String callJson(String system, String user, double temperature) {
        return callJson(system, user, temperature, 1);
    }

    /**
     * @param repairTimes 非法JSON时自动修复重试次数（建议 0~1）
     */
    public String callJson(String system, String user, double temperature, int repairTimes) {
        String out = doCall(system, user, temperature);
        if (isValidJsonObject(out)) return out;

        for (int i = 0; i < repairTimes; i++) {
            String repairSystem = "你是严格的JSON修复器。你必须只输出可解析的JSON对象，不能输出markdown或解释。";
            String repairUser = """
                    下面这段文本不是合法JSON对象。请在不新增字段的前提下，修复为一个可解析的JSON对象（只输出JSON对象）。
                    原始输出：
                    %s
                    """.formatted(truncate(out, 4000));
            out = doCall(repairSystem, repairUser, 0.0);
            if (isValidJsonObject(out)) return out;
        }

        throw new IllegalStateException("模型未返回合法JSON对象：" + truncate(out, 300));
    }

    private String doCall(String system, String user, double temperature) {
        var opts = DeepSeekChatOptions.builder()
                .temperature(temperature)
                .responseFormat(ResponseFormat.builder().type(ResponseFormat.Type.JSON_OBJECT).build())
                .build();

        Prompt prompt = new Prompt(
                List.of(new SystemMessage(system), new UserMessage(user)),
                opts
        );

        ChatResponse resp = chatModel.call(prompt);
        if (resp == null || resp.getResult() == null || resp.getResult().getOutput() == null) {
            throw new IllegalStateException("模型响应为空");
        }
        return resp.getResult().getOutput().getText();
    }

    private boolean isValidJsonObject(String s) {
        if (s == null || s.isBlank()) return false;
        try {
            JsonNode node = objectMapper.readTree(s);
            return node != null && node.isObject();
        } catch (Exception e) {
            return false;
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "";
        String x = s.replace("\r", " ").replace("\n", " ");
        return x.length() <= max ? x : x.substring(0, max) + "...";
    }
}
