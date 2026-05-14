package com.example.service.study;


import com.example.dto.study.course.CourseDetailDTO;
import com.example.dto.study.course.CourseHomeDTO;
import com.example.dto.study.course.CourseProgressDTO;
import com.example.mapper.study.course.*;
import com.example.pojo.study.course.Course;
import com.example.pojo.study.course.CourseFavorite;
import com.example.pojo.study.course.CourseLearning;
import com.example.pojo.study.course.CourseNote;
import com.example.pojo.study.course.CourseUserLearn;
import com.example.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class CourseService {
    @Autowired
    private CourseCategoryMapper courseCategoryMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseLearningMapper courseLearningMapper;
    @Autowired
    private CourseFavoriteMapper courseFavoriteMapper;
    @Autowired
    private CourseNoteMapper courseNoteMapper;

    public List<CourseHomeDTO.CategoryCard> listCategories() {
        return courseCategoryMapper.listCategoryCards();
    }

    public CourseHomeDTO getHomeData(String keyword, String difficulty, Integer categoryId,
                                     String myCoursesFilter, String month) {
        Integer userId = currentUserId();
        Integer teacherType = courseMapper.selectTeacherTypeByUserId(userId);
        Integer difficultyValue = normalizeDifficulty(difficulty);
        Integer myCourseStatus = normalizeMyCoursesFilter(myCoursesFilter);

        CourseHomeDTO dto = new CourseHomeDTO();
        dto.setCategories(courseCategoryMapper.listCategoryCards());
        dto.setFeaturedCourses(fillCourseRating(courseMapper.listFeaturedCourses(userId, teacherType, keyword, difficultyValue, categoryId, 6)));
        dto.setPopularCourses(fillCourseRating(courseMapper.listPopularCourses(userId, teacherType, keyword, difficultyValue, categoryId, 8)));
        dto.setMyCourses(courseLearningMapper.listMyCourses(userId, myCourseStatus));
        dto.setLearningRecords(fillLearningRecords(courseLearningMapper.listLearningRecords(userId, 5)));

        YearMonth yearMonth = (month == null || month.isBlank())
                ? YearMonth.now()
                : YearMonth.parse(month);

        LocalDate monthStart = yearMonth.atDay(1);
        LocalDate monthEnd = yearMonth.atEndOfMonth();

        dto.setCalendarDays(courseLearningMapper.listCalendarDays(userId, monthStart, monthEnd));
        dto.setDurationDistribution(courseLearningMapper.listDurationDistribution(userId, monthStart, monthEnd));
        dto.setTimeDistribution(courseLearningMapper.listTimeDistribution(userId, monthStart, monthEnd));

        dto.setStats(buildStats(userId));
        dto.setMyCoursesStats(buildMyCoursesStats(userId));
        dto.setCalendarStats(buildCalendarStats(userId, monthStart, monthEnd));
        dto.setEfficiencyStats(buildEfficiencyStats(userId, monthStart, monthEnd, dto.getMyCoursesStats()));
        return dto;
    }


    public CourseDetailDTO getCourseDetail(Integer courseId) {
        Integer userId = currentUserId();
        loadAvailableCourse(courseId);
        CourseDetailDTO detail = courseMapper.selectCourseDetail(userId, courseId);
        if (detail == null) {
            throw new RuntimeException("课程不存在或已下架");
        }
        detail.setProgress(detail.getProgress() == null ? 0 : detail.getProgress());
        detail.setLastSec(detail.getLastSec() == null ? 0 : detail.getLastSec());
        detail.setStudyTotal(detail.getStudyTotal() == null ? 0 : detail.getStudyTotal());
        detail.setFavorite(Boolean.TRUE.equals(detail.getFavorite()));
        return detail;
    }

    @Transactional
    public Map<String, Object> startCourse(Integer courseId) {
        Integer userId = currentUserId();
        Course course = loadAvailableCourse(courseId);
        CourseLearning learning = courseLearningMapper.selectByUserIdAndCourseId(userId, courseId);

        if (learning == null) {
            learning = new CourseLearning();
            learning.setUserId(userId);
            learning.setCourseId(courseId);
            learning.setStatus(1);
            learning.setProgressPercent(BigDecimal.ZERO);
            learning.setLastSec(0);
            learning.setStudyTotal(0);
            learning.setLastTime(LocalDateTime.now());
            learning.setStartedAt(LocalDateTime.now());
            learning.setCompletedAt(null);
            courseLearningMapper.insert(learning);
            courseMapper.incrementEnrollmentCount(courseId);
        } else {
            if (learning.getStartedAt() == null) {
                learning.setStartedAt(LocalDateTime.now());
            }
            learning.setStatus(1);
            learning.setLastTime(LocalDateTime.now());
            courseLearningMapper.update(learning);
        }

        Map<String, Object> out = new HashMap<>();
        out.put("courseId", course.getId());
        out.put("title", course.getTitle());
        out.put("status", 1);
        return out;
    }

    @Transactional
    public Map<String, Object> reviewCourse(Integer courseId) {
        Integer userId = currentUserId();
        loadAvailableCourse(courseId);
        CourseLearning learning = courseLearningMapper.selectByUserIdAndCourseId(userId, courseId);
        if (learning == null) {
            return startCourse(courseId);
        }
        courseLearningMapper.resetByUserIdAndCourseId(userId, courseId);

        Map<String, Object> out = new HashMap<>();
        out.put("courseId", courseId);
        out.put("status", 1);
        out.put("message", "已重置课程学习进度");
        return out;
    }

    @Transactional
    public Map<String, Object> updateProgress(CourseProgressDTO dto) {
        if (dto == null || dto.getCourseId() == null) {
            throw new RuntimeException("课程ID不能为空");
        }
        Integer userId = currentUserId();
        loadAvailableCourse(dto.getCourseId());
        LocalDateTime sessionStartTime = dto.getStartTime() == null ? LocalDateTime.now() : dto.getStartTime();
        LocalDateTime sessionEndTime = dto.getEndTime() == null ? LocalDateTime.now() : dto.getEndTime();

        CourseLearning learning = courseLearningMapper.selectByUserIdAndCourseId(userId, dto.getCourseId());
        if (learning == null) {
            learning = new CourseLearning();
            learning.setUserId(userId);
            learning.setCourseId(dto.getCourseId());
            learning.setStatus(1);
            learning.setProgressPercent(BigDecimal.ZERO);
            learning.setLastSec(0);
            learning.setStudyTotal(0);
            learning.setStartedAt(sessionStartTime);
            learning.setLastTime(LocalDateTime.now());
            courseLearningMapper.insert(learning);
            courseMapper.incrementEnrollmentCount(dto.getCourseId());
            learning = courseLearningMapper.selectByUserIdAndCourseId(userId, dto.getCourseId());
        }

        BigDecimal progressBefore = defaultDecimal(learning.getProgressPercent());
        BigDecimal progressAfter = dto.getProgressPercent() == null ? progressBefore : clampProgress(dto.getProgressPercent());
        int addStudySec = dto.getStudySec() == null ? 0 : Math.max(dto.getStudySec(), 0);
        int newStudyTotal = safeInt(learning.getStudyTotal()) + addStudySec;

        learning.setStatus(progressAfter.compareTo(BigDecimal.valueOf(100)) >= 0 ? 2 : (progressAfter.compareTo(BigDecimal.ZERO) > 0 ? 1 : 0));
        learning.setProgressPercent(progressAfter);
        learning.setLastSec(dto.getLastSec() == null ? safeInt(learning.getLastSec()) : Math.max(dto.getLastSec(), 0));
        learning.setStudyTotal(newStudyTotal);
        learning.setLastTime(LocalDateTime.now());
        if (learning.getStartedAt() == null) {
            learning.setStartedAt(sessionStartTime);
        }
        if (learning.getStatus() == 2) {
            learning.setCompletedAt(LocalDateTime.now());
        }
        courseLearningMapper.update(learning);

        if (addStudySec > 0) {
            CourseUserLearn record = courseLearningMapper.selectLearnRecordBySession(userId, dto.getCourseId(), sessionStartTime);
            if (record == null) {
                record = new CourseUserLearn();
                record.setUserId(userId);
                record.setCourseId(dto.getCourseId());
                record.setLearnDate(sessionStartTime.toLocalDate());
                record.setStartTime(sessionStartTime);
                record.setEndTime(sessionEndTime);
                record.setStudySec(addStudySec);
                record.setProgressBefore(progressBefore);
                record.setProgressAfter(progressAfter);
                courseLearningMapper.insertLearnRecord(record);
            } else {
                record.setEndTime(sessionEndTime);
                record.setStudySec(safeInt(record.getStudySec()) + addStudySec);
                record.setProgressAfter(progressAfter);
                courseLearningMapper.updateLearnRecord(record);
            }
        }

        Map<String, Object> out = new HashMap<>();
        out.put("courseId", dto.getCourseId());
        out.put("progress", progressAfter);
        out.put("status", learning.getStatus());
        out.put("studyTotal", newStudyTotal);
        return out;
    }

    @Transactional
    public Map<String, Object> toggleFavorite(Integer courseId) {
        Integer userId = currentUserId();
        loadAvailableCourse(courseId);
        CourseFavorite favorite = courseFavoriteMapper.selectByUserIdAndCourseId(userId, courseId);
        boolean favorited;
        if (favorite == null) {
            favorite = new CourseFavorite();
            favorite.setUserId(userId);
            favorite.setCourseId(courseId);
            courseFavoriteMapper.insert(favorite);
            favorited = true;
        } else {
            courseFavoriteMapper.deleteByUserIdAndCourseId(userId, courseId);
            favorited = false;
        }

        Map<String, Object> out = new HashMap<>();
        out.put("courseId", courseId);
        out.put("favorite", favorited);
        return out;
    }

    @Transactional
    public void removeMyCourse(Integer courseId) {
        Integer userId = currentUserId();
        courseLearningMapper.deleteByUserIdAndCourseId(userId, courseId);
    }

    public List<CourseNote> listNotes(Integer courseId) {
        loadAvailableCourse(courseId);
        return courseNoteMapper.listByUserIdAndCourseId(currentUserId(), courseId);
    }

    @Transactional
    public CourseNote addNote(CourseNote note) {
        if (note == null || note.getCourseId() == null) {
            throw new RuntimeException("课程ID不能为空");
        }
        if (note.getContent() == null || note.getContent().isBlank()) {
            throw new RuntimeException("笔记内容不能为空");
        }
        loadAvailableCourse(note.getCourseId());
        note.setUserId(currentUserId());
        courseNoteMapper.insert(note);
        return courseNoteMapper.selectById(note.getId());
    }

    @Transactional
    public void updateNote(CourseNote note) {
        if (note == null || note.getId() == null) {
            throw new RuntimeException("笔记ID不能为空");
        }
        if (note.getContent() == null || note.getContent().isBlank()) {
            throw new RuntimeException("笔记内容不能为空");
        }
        note.setUserId(currentUserId());
        if (courseNoteMapper.update(note) == 0) {
            throw new RuntimeException("笔记不存在或无权限修改");
        }
    }

    @Transactional
    public void deleteNote(Integer id) {
        if (courseNoteMapper.deleteByIdAndUserId(id, currentUserId()) == 0) {
            throw new RuntimeException("笔记不存在或无权限删除");
        }
    }

    private CourseHomeDTO.Stats buildStats(Integer userId) {
        CourseHomeDTO.Stats stats = new CourseHomeDTO.Stats();
        stats.setCompletedCourses(nullSafe(courseLearningMapper.countCompletedCourses(userId)));
        stats.setStudyHours(toHours(courseLearningMapper.sumStudySeconds(userId)));
        stats.setContinuityDays(calcContinuityDays(courseLearningMapper.listActiveDates(userId)));
        return stats;
    }

    private CourseHomeDTO.MyCoursesStats buildMyCoursesStats(Integer userId) {
        CourseHomeDTO.MyCoursesStats stats = new CourseHomeDTO.MyCoursesStats();
        int total = nullSafe(courseLearningMapper.countTotalCourses(userId));
        int completed = nullSafe(courseLearningMapper.countCompletedCourses(userId));
        int inProgress = nullSafe(courseLearningMapper.countInProgressCourses(userId));
        int notStarted = nullSafe(courseLearningMapper.countNotStartedCourses(userId));

        stats.setTotal(total);
        stats.setCompleted(completed);
        stats.setInProgress(inProgress);
        stats.setNotStarted(notStarted);
        stats.setCompletionRate(total <= 0 ? 0 : (int) Math.round(completed * 100.0 / total));
        return stats;
    }

    private CourseHomeDTO.CalendarStats buildCalendarStats(Integer userId, LocalDate startDate, LocalDate endDate) {
        CourseHomeDTO.CalendarStats stats = new CourseHomeDTO.CalendarStats();
        stats.getThisMonth().setTotalHours(toHours(courseLearningMapper.sumStudySecondsInRange(userId, startDate, endDate)));
        stats.getThisMonth().setCompletedCourses(nullSafe(courseLearningMapper.countCompletedCoursesInRange(userId, startDate, endDate)));
        return stats;
    }

    private CourseHomeDTO.EfficiencyStats buildEfficiencyStats(Integer userId, LocalDate startDate,
                                                               LocalDate endDate,
                                                               CourseHomeDTO.MyCoursesStats myCoursesStats) {
        CourseHomeDTO.EfficiencyStats stats = new CourseHomeDTO.EfficiencyStats();
        int avgSessionSec = nullSafe(courseLearningMapper.avgSessionStudySeconds(userId, startDate, endDate));
        int avgProgress = nullSafe(courseLearningMapper.avgProgressPercent(userId));

        int focus = clamp((int) Math.round(avgSessionSec * 100.0 / 1800));
        int completion = myCoursesStats == null || myCoursesStats.getCompletionRate() == null ? 0 : myCoursesStats.getCompletionRate();
        int comprehension = clamp(avgProgress);

        stats.setFocus(focus);
        stats.setCompletion(completion);
        stats.setComprehension(comprehension);
        return stats;
    }

    private List<CourseHomeDTO.CourseCard> fillCourseRating(List<CourseHomeDTO.CourseCard> courses) {
        if (courses == null) {
            return List.of();
        }
        for (CourseHomeDTO.CourseCard item : courses) {
            double rating = 4.5;
            if (item.getHotScore() != null) {
                rating = 4.5 + Math.min(item.getHotScore().doubleValue(), 10.0) / 20.0;
            }
            item.setRating(BigDecimal.valueOf(rating).setScale(1, RoundingMode.HALF_UP).doubleValue());
            item.setProgress(item.getProgress() == null ? 0 : item.getProgress());
            item.setFavorite(Boolean.TRUE.equals(item.getFavorite()));
        }
        return courses;
    }

    private List<CourseHomeDTO.RecordItem> fillLearningRecords(List<CourseHomeDTO.RecordItem> records) {
        if (records == null) {
            return List.of();
        }
        for (CourseHomeDTO.RecordItem item : records) {
            item.setType("video");
            item.setContent(buildRecordContent(item.getProgressBefore(), item.getProgressAfter(), item.getDuration()));
        }
        return records;
    }

    private String buildRecordContent(BigDecimal progressBefore, BigDecimal progressAfter, Integer duration) {
        BigDecimal before = defaultDecimal(progressBefore);
        BigDecimal after = defaultDecimal(progressAfter);
        if (after.compareTo(before) > 0) {
            return "完成一次视频学习，学习进度由" + before.intValue() + "%提升至" + after.intValue() + "%";
        }
        if (duration != null && duration > 0) {
            return "完成一次视频学习，本次学习时长约" + duration + "分钟";
        }
        return "完成一次视频学习";
    }

    private int calcContinuityDays(List<LocalDate> activeDates) {
        if (activeDates == null || activeDates.isEmpty()) {
            return 0;
        }
        int days = 1;
        LocalDate prev = activeDates.get(0);
        for (int i = 1; i < activeDates.size(); i++) {
            LocalDate current = activeDates.get(i);
            if (prev.minusDays(1).equals(current)) {
                days++;
                prev = current;
            } else {
                break;
            }
        }
        return days;
    }

    private Course loadAvailableCourse(Integer courseId) {
        Course course = courseMapper.selectById(courseId);
        if (course == null || (course.getStatus() != null && course.getStatus() == 0)) {
            throw new RuntimeException("课程不存在或已下架");
        }
        return course;
    }

    private Integer normalizeDifficulty(String difficulty) {
        if (difficulty == null || difficulty.isBlank() || "all".equalsIgnoreCase(difficulty)) {
            return null;
        }
        return switch (difficulty) {
            case "1", "beginner" -> 1;
            case "2", "intermediate" -> 2;
            case "3", "advanced" -> 3;
            default -> null;
        };
    }

    private Integer normalizeMyCoursesFilter(String filter) {
        if (filter == null || filter.isBlank() || "all".equalsIgnoreCase(filter)) {
            return null;
        }
        return switch (filter) {
            case "not-started" -> 0;
            case "in-progress" -> 1;
            case "completed" -> 2;
            default -> null;
        };
    }

    private Integer currentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) {
            throw new SecurityException("未登录");
        }
        return userId;
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private double toHours(Integer seconds) {
        if (seconds == null) {
            return 0D;
        }
        return BigDecimal.valueOf(seconds / 3600.0)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private int clamp(int value) {
        return Math.max(0, Math.min(100, value));
    }

    private BigDecimal clampProgress(BigDecimal progress) {
        if (progress == null) {
            return BigDecimal.ZERO;
        }
        if (progress.compareTo(BigDecimal.ZERO) < 0) {
            return BigDecimal.ZERO;
        }
        if (progress.compareTo(BigDecimal.valueOf(100)) > 0) {
            return BigDecimal.valueOf(100);
        }
        return progress.setScale(2, RoundingMode.HALF_UP);
    }

    private BigDecimal defaultDecimal(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}
