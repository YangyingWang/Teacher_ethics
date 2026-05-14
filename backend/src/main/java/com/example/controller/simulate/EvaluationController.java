package com.example.controller.simulate;

import com.example.dto.simulate.EvaluationDetailDTO;
import com.example.dto.simulate.EvaluationHomeDTO;
import com.example.pojo.Result;
import com.example.pojo.simulate.evaluation.Evaluation;
import com.example.pojo.simulate.evaluation.EvaluationDimension;
import com.example.service.simulate.EvaluationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/simulate/evaluation")
public class EvaluationController {
    @Autowired
    private EvaluationService evaluationService;

    @PostMapping()
    public Result<Integer> ensure(Integer sessionId) {
        return Result.success(evaluationService.ensureEvaluation(sessionId));
    }

    @GetMapping
    public Result<EvaluationDetailDTO> detail(Integer sessionId) {
        return Result.success(evaluationService.detail(sessionId));
    }

    @GetMapping("/dimensions")
    public Result<List<EvaluationDimension>> dimensions(){
        List<EvaluationDimension> dss = evaluationService.getDimensions();
        return Result.success(dss);
    }

    @GetMapping("/recent")
    public Result<List<Evaluation>> recent() {
        return Result.success(evaluationService.getRecent());
    }

    @GetMapping("/home")
    public Result<EvaluationHomeDTO> home() {
        return Result.success(evaluationService.getHome());
    }
//    @GetMapping("/list")
//    public Result<PageBean<Evaluation>> list(Integer pageNum, Integer pageSize) {
//        PageBean<Evaluation> pq = evaluationService.list(pageNum, pageSize);
//        return Result.success(pq);
//    }
}
