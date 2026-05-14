package com.example.utils;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class JsonUtil {
    private static final ObjectMapper om = new ObjectMapper();

    public static String toJson(Object o) {
        try {
            return om.writeValueAsString(o);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    public static Integer toInt(Object o, Integer def) {
        if (o == null) return def;
        if (o instanceof Number n) return n.intValue();
        try {
            return Integer.parseInt(String.valueOf(o));
        } catch (Exception e) {
            return def;
        }
    }

    public static String asText(Object o) {
        return o == null ? null : String.valueOf(o);
    }

    public static List<Map<String, Object>> asListMap(Object o) {
        if (o instanceof List<?> l) {
            List<Map<String, Object>> r = new ArrayList<>();
            for (Object x : l) {
                if (x instanceof Map<?, ?> m) r.add((Map<String, Object>) m);
            }
            return r;
        }
        return List.of();
    }

    // ---------- json helpers ----------
    public static Map<String, Object> parseMap(String json) {
        try {
            return om.readValue(json, new TypeReference<Map<String, Object>>() {
            });
        } catch (Exception e) {
            return new HashMap<>();
        }
    }

    public static List<Integer> parseIds(String json) {
        try {
            return om.readValue(json, new TypeReference<List<Integer>>() {
            });
        } catch (Exception e) {
            return List.of();
        }
    }

    public static List<String> parseListString(String json) {
        if (json == null || json.trim().isEmpty()) return new ArrayList<>();
        try {
            return om.readValue(json, new TypeReference<List<String>>() {});
        } catch (Exception e) {
            return new ArrayList<>();
        }
    }

    public static Object parseAny(String json) {
        try {
            return om.readValue(json, Object.class);
        } catch (Exception e) {
            return json;
        }
    }

    public static BigDecimal parseWeight(Object obj) {
        if (obj == null) return null;
        try {
            BigDecimal x = new BigDecimal(String.valueOf(obj));
            return x.setScale(2, RoundingMode.HALF_UP);
        } catch (Exception e) {
            return null;
        }
    }
}
