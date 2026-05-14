package com.example.controller.test;

import com.example.dto.test.QuestionDTO;
import com.example.dto.test.SubmitRequest;
import com.example.pojo.Result;
import com.example.service.test.QuizService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/test/quiz")
public class QuizController {
    @Autowired
    private QuizService quizService;

    @PostMapping("/start")
    public Result<?> createQn(Integer sceneId) {
        Map<String, Object> data = quizService.createQuestionnaire(sceneId);
        return Result.success(data);
    }

    @GetMapping("/questions")
    public Result<List<QuestionDTO>> getQuestions(Integer qnId) {
        return Result.success(quizService.getQuestions(qnId));
    }

    @PostMapping("/submit")
    public Result<?> submit(@RequestParam Integer qnId, @RequestBody SubmitRequest req) {
        return Result.success(quizService.submit(qnId, req));
    }

    @DeleteMapping
    public Result abandon(Integer qnId) {
        quizService.abandon(qnId);
        return Result.success();
    }
}
