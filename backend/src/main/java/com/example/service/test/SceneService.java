package com.example.service.test;

import com.example.mapper.test.SceneMapper;
import com.example.pojo.test.SceneCategory;
import com.example.pojo.test.Scene;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class SceneService {
    @Autowired
    private SceneMapper sceneMapper;

    public Scene findSceneDetailById(Integer id){
        return sceneMapper.findSceneDetailById(id);
    }
    public List<Scene> getAllScenesForSelect() {
        return sceneMapper.allScenesForSelect();
    }

    public List<SceneCategory> getAllSceneCategories() {
        return sceneMapper.allSceneCategories();
    }
}
