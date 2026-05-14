package com.example.controller.test;

import com.example.pojo.Result;
import com.example.pojo.test.SceneCategory;
import com.example.pojo.test.Scene;
import com.example.service.test.SceneService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/test/scene")
public class SceneController {
    @Autowired
    private SceneService sceneService;

    @GetMapping("/info")
    public Result<Scene> findSceneDetailById(Integer id){
        Scene scene = sceneService.findSceneDetailById(id);
        return Result.success(scene);
    }

    @GetMapping("/list")
    public Result<List<Scene>> getSceneList(){
        List<Scene> s = sceneService.getAllScenesForSelect();
        return Result.success(s);
    }

    @GetMapping("/categories")
    public Result<List<SceneCategory>> getSceneCategories(){
        List<SceneCategory> c = sceneService.getAllSceneCategories();
        return Result.success(c);
    }
}
