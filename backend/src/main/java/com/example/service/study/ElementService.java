package com.example.service.study;

import com.example.dto.study.element.*;
import com.example.mapper.study.element.ElementFavoriteMapper;
import com.example.mapper.study.element.ElementSearchMapper;
import com.example.pojo.study.element.CourseType;
import com.example.pojo.study.element.Discipline;
import com.example.pojo.study.element.ElementType;
import com.example.pojo.study.element.TeachingCourse;
import com.example.utils.ThreadLocalUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class ElementService {

    @Autowired
    private ElementSearchMapper ideologySearchMapper;
    @Autowired
    private ElementFavoriteMapper elementFavoriteMapper;

    public ElementHomeDTO home() {
        Integer userId = currentUserId();
        SearchUserProfile profile = ideologySearchMapper.selectUserProfile(userId);

        ElementHomeDTO dto = new ElementHomeDTO();
        dto.setElementTypes(toOptionsFromElementType(ideologySearchMapper.listEnabledElementTypes()));
        dto.setDisciplines(toOptionsFromDiscipline(ideologySearchMapper.listEnabledDisciplines()));
        dto.setCourseTypes(toOptionsFromCourseType(ideologySearchMapper.listEnabledCourseTypes()));
        dto.setTeachingCourses(toOptionsFromTeachingCourse(
                ideologySearchMapper.listTeachingCoursesByDiscipline(profile == null ? null : profile.getDisciplineId())
        ));
        dto.setSuggestedKeywords(buildSuggestedKeywords());
        dto.setRecommendations(buildRecommendations(userId, profile, 4));
        dto.setFavoriteList(elementFavoriteMapper.listLatestFavorites(userId, 3));

        ElementHomeDTO.Stats stats = new ElementHomeDTO.Stats();
        stats.setTotalElements(nullSafe(ideologySearchMapper.countEnabledElements()));
        stats.setTotalCourses(nullSafe(ideologySearchMapper.countEnabledTeachingCourses()));
        stats.setFavoriteCount(nullSafe(elementFavoriteMapper.countByUserId(userId)));
        dto.setStats(stats);
        return dto;
    }

    public ElementPageDTO page(SearchQueryDTO query) {
        Integer userId = currentUserId();
        SearchUserProfile profile = ideologySearchMapper.selectUserProfile(userId);

        if (query == null) {
            query = new SearchQueryDTO();
        }
        normalizeQuery(query);

        Long total = ideologySearchMapper.countPage(query, userId);
        int offset = (query.getPageNum() - 1) * query.getPageSize();

        List<ElementRow> rows = ideologySearchMapper.pageRows(
                query,
                userId,
                profile == null ? null : profile.getType(),
                profile == null ? null : profile.getDisciplineId(),
                offset,
                query.getPageSize()
        );

        ElementPageDTO dto = new ElementPageDTO();
        dto.setTotal(total == null ? 0L : total);
        dto.setPageNum(query.getPageNum());
        dto.setPageSize(query.getPageSize());
        dto.setList(rows.stream().map(this::toCardDTO).collect(Collectors.toList()));
        return dto;
    }

    @Transactional
    public ElementDetailDTO detail(Integer elementId) {
        Integer userId = currentUserId();
        ideologySearchMapper.increaseViewCount(elementId);

        ElementRow row = ideologySearchMapper.selectDetailRow(elementId, userId);
        if (row == null) {
            throw new RuntimeException("思政元素不存在");
        }

        ElementDetailDTO dto = new ElementDetailDTO();
        dto.setId(row.getId());
        dto.setTitle(row.getTitle());
        dto.setSummary(row.getSummary());
        dto.setContent(row.getContent());
        dto.setDifficulty(row.getDifficulty());
        dto.setIdeologyType(row.getElementTypeName());
        dto.setKeywords(splitKeywords(row.getKeywords()));
        dto.setSuitableCourses(ideologySearchMapper.listTeachingCourseNamesByElementId(row.getId()));
        dto.setDisciplines(ideologySearchMapper.listDisciplineNamesByElementId(row.getId()));
        dto.setCourseTypes(ideologySearchMapper.listCourseTypeNamesByElementId(row.getId()));
        dto.setViewCount(nullSafe(row.getViewCount()) + 1);
        dto.setFavoriteCount(nullSafe(row.getFavoriteCount()));
        dto.setUseCount(nullSafe(row.getUseCount()));
        dto.setPopularity(popularity(row.getHotScore()));
        dto.setCollected(flag(row.getCollectedFlag()));
        return dto;
    }

    @Transactional
    public Boolean toggleFavorite(Integer elementId) {
        Integer userId = currentUserId();
        ElementRow row = ideologySearchMapper.selectDetailRow(elementId, userId);
        if (row == null) {
            throw new RuntimeException("思政元素不存在");
        }

        boolean exists = nullSafe(elementFavoriteMapper.exists(userId, elementId)) > 0;
        if (exists) {
            elementFavoriteMapper.delete(userId, elementId);
            ideologySearchMapper.decreaseFavoriteCount(elementId);
            return false;
        }

        elementFavoriteMapper.insert(userId, elementId);
        ideologySearchMapper.increaseFavoriteCount(elementId);
        return true;
    }

    public List<FavoritePreviewDTO> favorites() {
        Integer userId = currentUserId();
        return elementFavoriteMapper.listLatestFavorites(userId, 100);
    }

    public List<OptionDTO> teachingCourses() {
        Integer userId = currentUserId();
        SearchUserProfile profile = ideologySearchMapper.selectUserProfile(userId);
        return toOptionsFromTeachingCourse(
                ideologySearchMapper.listTeachingCoursesByDiscipline(profile == null ? null : profile.getDisciplineId())
        );
    }

    private List<ElementCardDTO> buildRecommendations(Integer userId, SearchUserProfile profile, int limit) {
        Integer teacherType = profile == null ? null : profile.getType();
        Integer disciplineId = profile == null ? null : profile.getDisciplineId();

        List<Integer> favoriteTypeIds = elementFavoriteMapper.listFavoriteElementTypeIds(userId);
        Set<Integer> favoriteTypeSet = new HashSet<>(favoriteTypeIds == null ? Collections.emptyList() : favoriteTypeIds);

        List<ElementRow> candidates = ideologySearchMapper.listRecommendationCandidates(
                userId, teacherType, disciplineId, 20
        );

        candidates.sort((a, b) -> Double.compare(
                calcRecommendationWeight(b, favoriteTypeSet),
                calcRecommendationWeight(a, favoriteTypeSet)
        ));

        return candidates.stream()
                .limit(limit)
                .map(row -> {
                    ElementCardDTO card = toCardDTO(row);
                    card.setRecommendationScore(roundScore(calcRecommendationWeight(row, favoriteTypeSet) / 10.0));
                    return card;
                })
                .collect(Collectors.toList());
    }

    private double calcRecommendationWeight(ElementRow row, Set<Integer> favoriteTypeSet) {
        double score = row.getHotScore() == null ? 0D : row.getHotScore().doubleValue();
        if (flag(row.getTeacherMatchedFlag())) {
            score += 8D;
        }
        if (flag(row.getDisciplineMatchedFlag())) {
            score += 6D;
        }
        if (row.getElementTypeId() != null && favoriteTypeSet.contains(row.getElementTypeId())) {
            score += 5D;
        }
        if (flag(row.getCollectedFlag())) {
            score += 2D;
        }
        return score;
    }

    private ElementCardDTO toCardDTO(ElementRow row) {
        ElementCardDTO dto = new ElementCardDTO();
        dto.setId(row.getId());
        dto.setTitle(row.getTitle());
        dto.setDescription(row.getSummary());
        dto.setDifficulty(row.getDifficulty());
        dto.setIdeologyType(row.getElementTypeName());
        dto.setSuitableCourses(ideologySearchMapper.listTeachingCourseNamesByElementId(row.getId()));
        dto.setKeywords(splitKeywords(row.getKeywords()));
        dto.setRecommendationScore(roundScore((row.getHotScore() == null ? 0D : row.getHotScore().doubleValue()) / 10.0));
        dto.setCollected(flag(row.getCollectedFlag()));
        dto.setPopularity(popularity(row.getHotScore()));
        return dto;
    }

    private List<String> buildSuggestedKeywords() {
        List<String> keywordSources = ideologySearchMapper.listHotKeywordSources(20);
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String source : keywordSources) {
            for (String item : splitKeywords(source)) {
                freq.put(item, freq.getOrDefault(item, 0) + 1);
            }
        }

        return freq.entrySet().stream()
                .sorted((a, b) -> {
                    int cmp = Integer.compare(b.getValue(), a.getValue());
                    if (cmp != 0) {
                        return cmp;
                    }
                    return a.getKey().compareTo(b.getKey());
                })
                .limit(8)
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
    }

    private List<String> splitKeywords(String keywords) {
        if (keywords == null || keywords.isBlank()) {
            return Collections.emptyList();
        }
        return Arrays.stream(keywords.split("[,，]"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .distinct()
                .collect(Collectors.toList());
    }

    private void normalizeQuery(SearchQueryDTO query) {
        if (query.getPageNum() == null || query.getPageNum() < 1) {
            query.setPageNum(1);
        }
        if (query.getPageSize() == null || query.getPageSize() < 1) {
            query.setPageSize(12);
        }
        if (query.getPageSize() > 50) {
            query.setPageSize(50);
        }
        if (query.getSortBy() == null || query.getSortBy().isBlank()) {
            query.setSortBy("relevance");
        }
        if (query.getOnlyFavorite() == null) {
            query.setOnlyFavorite(false);
        }
        if (query.getKeyword() != null) {
            query.setKeyword(query.getKeyword().trim());
        }
    }

    private Integer currentUserId() {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        if (userId == null) {
            throw new SecurityException("未登录");
        }
        return userId;
    }

    private boolean flag(Integer value) {
        return value != null && value > 0;
    }

    private int nullSafe(Integer value) {
        return value == null ? 0 : value;
    }

    private Integer popularity(BigDecimal hotScore) {
        if (hotScore == null) {
            return 0;
        }
        return hotScore.setScale(0, RoundingMode.HALF_UP).intValue();
    }

    private Double roundScore(double value) {
        return BigDecimal.valueOf(value)
                .setScale(1, RoundingMode.HALF_UP)
                .doubleValue();
    }

    private List<OptionDTO> toOptionsFromElementType(List<ElementType> list) {
        return list.stream()
                .map(x -> new OptionDTO(x.getId(), x.getName()))
                .collect(Collectors.toList());
    }

    private List<OptionDTO> toOptionsFromDiscipline(List<Discipline> list) {
        return list.stream()
                .map(x -> new OptionDTO(x.getId(), x.getName()))
                .collect(Collectors.toList());
    }

    private List<OptionDTO> toOptionsFromCourseType(List<CourseType> list) {
        return list.stream()
                .map(x -> new OptionDTO(x.getId(), x.getName()))
                .collect(Collectors.toList());
    }

    private List<OptionDTO> toOptionsFromTeachingCourse(List<TeachingCourse> list) {
        return list.stream()
                .map(x -> new OptionDTO(x.getId(), x.getName()))
                .collect(Collectors.toList());
    }
}
