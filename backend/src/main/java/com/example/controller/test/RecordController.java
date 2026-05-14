package com.example.controller.test;

import com.example.dto.test.QuizResultDTO;
import com.example.pojo.PageBean;
import com.example.pojo.Result;
import com.example.pojo.test.quiz.Questionnaire;
import com.example.service.test.RecordService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test/record")
public class RecordController {
    @Autowired
    private RecordService recordService;

    @GetMapping
    public Result<QuizResultDTO> result(Integer qnId) {
        return Result.success(recordService.getResult(qnId));
    }

    @GetMapping("/recent")
    public Result<List<Questionnaire>> recent() {
        return Result.success(recordService.getRecent());
    }

    @GetMapping("/list")
    public Result<PageBean<Questionnaire>> list(Integer pageNum, Integer pageSize) {
        PageBean<Questionnaire> pq = recordService.list(pageNum, pageSize);
        return Result.success(pq);
    }

    @DeleteMapping
    public Result delete(Integer qnId) {
        recordService.deleteByQnId(qnId);
        return Result.success();
    }
}
