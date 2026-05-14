package com.example.dto.study.course;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
public class CourseHomeDTO {
    private Stats stats = new Stats();
    private MyCoursesStats myCoursesStats = new MyCoursesStats();
    private CalendarStats calendarStats = new CalendarStats();
    private EfficiencyStats efficiencyStats = new EfficiencyStats();

    private List<CategoryCard> categories = new ArrayList<>();
    private List<CourseCard> featuredCourses = new ArrayList<>();
    private List<CourseCard> popularCourses = new ArrayList<>();
    private List<MyCourseItem> myCourses = new ArrayList<>();
    private List<RecordItem> learningRecords = new ArrayList<>();
    private List<CalendarDay> calendarDays = new ArrayList<>();
    private List<ChartItem> durationDistribution = new ArrayList<>();
    private List<ChartItem> timeDistribution = new ArrayList<>();

    @Data
    public static class Stats {
        private Integer completedCourses;
        private Double studyHours;
        private Integer continuityDays;
    }

    @Data
    public static class MyCoursesStats {
        private Integer total;
        private Integer completed;
        private Integer inProgress;
        private Integer notStarted;
        private Integer completionRate;
    }

    @Data
    public static class CalendarStats {
        private ThisMonth thisMonth = new ThisMonth();

        @Data
        public static class ThisMonth {
            private Double totalHours;
            private Integer completedCourses;
        }
    }

    @Data
    public static class EfficiencyStats {
        private Integer focus;
        private Integer completion;
        private Integer comprehension;
    }

    @Data
    public static class CategoryCard {
        private Integer id;
        private String name;
        private String description;
        private Integer courseCount;
        private Double studyHours;
    }

    @Data
    public static class CourseCard {
        private Integer id;
        private String title;
        private String description;
        private String cover;
        private String videoUrl;
        private Integer difficulty;
        private Integer categoryId;
        private String categoryName;
        private Integer duration;
        private Integer enrollment;
        private BigDecimal hotScore;
        private Double rating;
        private Integer progress;
        private Boolean favorite;
    }

    @Data
    public static class MyCourseItem {
        private Integer id;
        private String title;
        private String cover;
        private String videoUrl;
        private Integer categoryId;
        private String categoryName;
        private Integer duration;
        private Integer progress;
        private Boolean favorite;
        private LocalDateTime lastStudyTime;
    }

    @Data
    public static class RecordItem {
        private Integer id;
        private Integer courseId;
        private String courseTitle;
        private String type;
        private String content;
        private Integer duration;
        private LocalDateTime timestamp;
        private BigDecimal progressBefore;
        private BigDecimal progressAfter;
    }

    @Data
    public static class CalendarDay {
        private LocalDate date;
        private Double studyHours;
    }

    @Data
    public static class ChartItem {
        private String label;
        private Double value;
    }
}
