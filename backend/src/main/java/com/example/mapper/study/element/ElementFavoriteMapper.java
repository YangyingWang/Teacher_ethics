package com.example.mapper.study.element;

import com.example.dto.study.element.FavoritePreviewDTO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ElementFavoriteMapper {

    @Select("SELECT COUNT(*) FROM element_favorite WHERE user_id = #{userId}")
    Integer countByUserId(Integer userId);

    @Select("SELECT COUNT(*) FROM element_favorite WHERE user_id = #{userId} AND element_id = #{elementId} ")
    Integer exists(Integer userId, Integer elementId);

    @Insert("INSERT INTO element_favorite(user_id, element_id, created_at) VALUES(#{userId}, #{elementId}, NOW()) ")
    int insert(Integer userId, Integer elementId);

    @Delete("DELETE FROM element_favorite WHERE user_id = #{userId} AND element_id = #{elementId} ")
    int delete(Integer userId, Integer elementId);

    @Select("""
            SELECT DISTINCT e.element_type_id
            FROM element_favorite ef
            JOIN elements e ON e.id = ef.element_id
            WHERE ef.user_id = #{userId}
              AND e.status = 1
            ORDER BY e.element_type_id ASC
            """)
    List<Integer> listFavoriteElementTypeIds(Integer userId);

    @Select("""
            SELECT
                ef.element_id AS id,
                e.title AS title,
                et.name AS element_type_name,
                ef.created_at AS collected_at
            FROM element_favorite ef
            JOIN elements e ON e.id = ef.element_id
            JOIN element_types et ON et.id = e.element_type_id
            WHERE ef.user_id = #{userId}
              AND e.status = 1
            ORDER BY ef.created_at DESC
            LIMIT #{limit}
            """)
    List<FavoritePreviewDTO> listLatestFavorites(Integer userId, Integer limit);
}
