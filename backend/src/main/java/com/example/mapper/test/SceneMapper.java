package com.example.mapper.test;

import com.example.pojo.test.SceneCategory;
import com.example.pojo.test.Scene;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface SceneMapper {
    //获取情景列表(详细)
    @Select("select * from scenes where id=#{id}")
    Scene findSceneDetailById(Integer id);

    //获取情景列表(简单，用于下拉框)
    @Select("select * from scenes")
    List<Scene> allScenesForSelect();

    //获取所有情景分类
    @Select("select * from scene_category")
    List<SceneCategory> allSceneCategories();

    //根据分类ID获取分类名称
    @Select("select name from scene_category where id=#{id}")
    String findCategoryNameById(Integer id);
}
