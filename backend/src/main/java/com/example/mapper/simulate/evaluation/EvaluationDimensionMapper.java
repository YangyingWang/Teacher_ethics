package com.example.mapper.simulate.evaluation;

import com.example.pojo.simulate.evaluation.EvaluationDimension;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface EvaluationDimensionMapper {
    @Select("SELECT * FROM evaluation_dimensions")
    List<EvaluationDimension> listAll();

    @Select("SELECT * FROM evaluation_dimensions WHERE id = #{id}")
    EvaluationDimension selectById(Integer id);

    @Select("SELECT * FROM evaluation_dimensions WHERE type = #{type}")
    List<EvaluationDimension> listByType(Integer type);
}
