package com.investinator.domain;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import java.util.*;
public final class Json {
    private static final ObjectMapper MAPPER = new ObjectMapper();
    private Json() {}
    public static String encode(Object value) {
        try { return MAPPER.writeValueAsString(value); } catch (Exception e) { throw new IllegalArgumentException("Invalid JSON value", e); }
    }
    public static <T> T decode(String value, Class<T> type) {
        try { return MAPPER.readValue(value, type); } catch (Exception e) { throw new IllegalArgumentException("Invalid stored JSON", e); }
    }
    public static Object decode(String value) { return decode(value,Object.class); }
    @SuppressWarnings("unchecked") public static Map<String,Object> map(Object value) { return (Map<String,Object>)value; }
    @SuppressWarnings("unchecked") public static List<Map<String,Object>> rows(Object value) { return (List<Map<String,Object>>)value; }
    public static Map<String,Object> obj(Object... pairs) {
        var map = new LinkedHashMap<String,Object>();
        for (int i=0;i<pairs.length;i+=2) map.put((String)pairs[i],pairs[i+1]);
        return map;
    }
    public static String text(Map<String,Object> row,String key) { return Objects.toString(row.get(key),""); }
    public static String id() { return UUID.randomUUID().toString(); }
    public static String now() { return java.time.Instant.now().toString(); }
}
