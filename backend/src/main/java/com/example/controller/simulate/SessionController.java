package com.example.controller.simulate;

import com.example.pojo.Result;
import com.example.pojo.simulate.chat.ChatSession;
import com.example.service.simulate.SessionService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@RestController
@RequestMapping("/simulate/chat/session")
public class SessionController {
    @Autowired
    private SessionService sessionService;

    @PostMapping
    public Result<ChatSession> create(@RequestParam(required = false) String title,
                                      @RequestParam(required = false) Integer maxSteps,
                                      @RequestParam(required = false) Integer sceneCategoryId) {
        ChatSession session = sessionService.createSession(title, maxSteps, sceneCategoryId);
        return Result.success(session);
    }

    @DeleteMapping
    public Result delete(Integer id) {
        boolean ok = sessionService.deleteSession(id);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "该会话不存在！");
        }
        return Result.success();
    }

    @GetMapping("/list")
    public Result<List<ChatSession>> list() {
        List<ChatSession> sessions = sessionService.listSessions();
        return Result.success(sessions);
    }

    @GetMapping
    public Result<ChatSession> detail(@RequestParam Integer id) {
        ChatSession session = sessionService.getSession(id);
        return Result.success(session);
    }

    @PutMapping("/title")
    public Result updateTitle(@RequestParam Integer id, @RequestParam String title) {
        sessionService.updateTitle(id, title);
        return Result.success();
    }
}
