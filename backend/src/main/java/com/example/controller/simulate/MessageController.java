package com.example.controller.simulate;

import com.example.pojo.Result;
import com.example.pojo.simulate.chat.ChatMessage;
import com.example.service.simulate.MessageService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/simulate/chat/message")
public class MessageController {
    @Autowired
    private MessageService messageService;

    @GetMapping
    public Result<List<ChatMessage>> list(Integer sessionId) {
        List<ChatMessage> messages = messageService.getMessagesBySessionId(sessionId);
        return Result.success(messages);
    }
}