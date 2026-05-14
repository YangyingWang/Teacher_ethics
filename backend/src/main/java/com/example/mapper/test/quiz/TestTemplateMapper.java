package com.example.mapper.test.quiz;

import com.example.pojo.test.quiz.TestTemplate;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Select;

@Mapper
public interface TestTemplateMapper {
    @Select("select * from test_templates where scene_id=#{sceneId} ")
    TestTemplate selectBySceneId(Integer sceneId);

    @Insert(" INSERT INTO test_templates(scene_id, title, question_sequence, total_score, total_count, raw_json, created_at) " +
            "VALUES(#{sceneId}, #{title}, #{questionSequence}, #{totalScore}, #{totalCount}, #{rawJson}, NOW()) ")
    @Options(useGeneratedKeys = true, keyProperty = "id", keyColumn = "id")
    int insert(TestTemplate t);
}
