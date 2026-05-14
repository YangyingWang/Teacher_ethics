package com.example.controller.study;

import com.example.dto.study.element.FavoritePreviewDTO;
import com.example.dto.study.element.ElementDetailDTO;
import com.example.dto.study.element.ElementHomeDTO;
import com.example.dto.study.element.ElementPageDTO;
import com.example.dto.study.element.SearchQueryDTO;
import com.example.dto.study.element.OptionDTO;
import com.example.pojo.Result;
import com.example.service.study.ElementService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/study/element")
public class ElementController {

    @Autowired
    private ElementService ideologySearchService;

    @GetMapping("/home")
    public Result<ElementHomeDTO> home() {
        return Result.success(ideologySearchService.home());
    }

    @GetMapping("/page")
    public Result<ElementPageDTO> page(SearchQueryDTO query) {
        return Result.success(ideologySearchService.page(query));
    }

    @GetMapping("/favorites")
    public Result<List<FavoritePreviewDTO>> favorites() {
        return Result.success(ideologySearchService.favorites());
    }

    @GetMapping("/teaching-courses")
    public Result<List<OptionDTO>> teachingCourses() {
        return Result.success(ideologySearchService.teachingCourses());
    }

    @GetMapping("/{id}")
    public Result<ElementDetailDTO> detail(@PathVariable Integer id) {
        return Result.success(ideologySearchService.detail(id));
    }

    @PostMapping("/{id}/favorite")
    public Result<Boolean> toggleFavorite(@PathVariable Integer id) {
        return Result.success(ideologySearchService.toggleFavorite(id));
    }
}
