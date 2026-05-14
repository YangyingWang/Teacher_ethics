package com.example.controller.study;

import com.example.dto.study.course.CourseDetailDTO;
import com.example.dto.study.course.CourseHomeDTO;
import com.example.dto.study.course.CourseProgressDTO;
import com.example.pojo.Result;
import com.example.pojo.study.course.CourseNote;
import com.example.service.study.CourseService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/study/course")
public class CourseController {
    @Autowired
    private CourseService courseService;

    @GetMapping("/categories")
    public Result<List<CourseHomeDTO.CategoryCard>> categories() {
        return Result.success(courseService.listCategories());
    }

    @GetMapping("/home")
    public Result<CourseHomeDTO> home(@RequestParam(required = false) String keyword,
                                      @RequestParam(required = false) String difficulty,
                                      @RequestParam(required = false) Integer categoryId,
                                      @RequestParam(required = false, defaultValue = "all") String myCoursesFilter,
                                      @RequestParam(required = false) String month) {
        return Result.success(courseService.getHomeData(keyword, difficulty, categoryId, myCoursesFilter, month));
    }

    @GetMapping("/detail")
    public Result<CourseDetailDTO> detail(@RequestParam Integer courseId) {
        return Result.success(courseService.getCourseDetail(courseId));
    }

    @PostMapping("/start")
    public Result<Map<String, Object>> start(@RequestParam Integer courseId) {
        return Result.success(courseService.startCourse(courseId));
    }

    @PostMapping("/review")
    public Result<Map<String, Object>> review(@RequestParam Integer courseId) {
        return Result.success(courseService.reviewCourse(courseId));
    }

    @PostMapping("/progress")
    public Result<Map<String, Object>> progress(@RequestBody CourseProgressDTO dto) {
        return Result.success(courseService.updateProgress(dto));
    }

    @PostMapping("/favorite/toggle")
    public Result<Map<String, Object>> toggleFavorite(@RequestParam Integer courseId) {
        return Result.success(courseService.toggleFavorite(courseId));
    }

    @PostMapping("/remove")
    public Result<String> removeMyCourse(@RequestParam Integer courseId) {
        courseService.removeMyCourse(courseId);
        return Result.success("移除成功");
    }

    @GetMapping("/note/list")
    public Result<List<CourseNote>> listNotes(@RequestParam Integer courseId) {
        return Result.success(courseService.listNotes(courseId));
    }

    @PostMapping("/note")
    public Result<CourseNote> addNote(@RequestBody CourseNote note) {
        return Result.success(courseService.addNote(note));
    }

    @PutMapping("/note")
    public Result<String> updateNote(@RequestBody CourseNote note) {
        courseService.updateNote(note);
        return Result.success("更新成功");
    }

    @DeleteMapping("/note")
    public Result<String> deleteNote(@RequestParam Integer id) {
        courseService.deleteNote(id);
        return Result.success("删除成功");
    }
}
