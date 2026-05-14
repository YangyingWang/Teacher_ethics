package com.example.controller.simulate;

import com.example.dto.simulate.ChatReply;
import com.example.dto.simulate.ChatRequest;
import com.example.pojo.Result;
import com.example.service.simulate.ChatService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/simulate/chat")
public class ChatController {
    @Autowired
    private ChatService chatService;

    @PostMapping("/ask")
    public Result<ChatReply> chat(@RequestBody ChatRequest req) {
        ChatReply reply = chatService.chat(req);
        return Result.success(reply);
    }
}
