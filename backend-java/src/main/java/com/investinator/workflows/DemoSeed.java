package com.investinator.workflows;
import com.investinator.domain.Json;
import java.util.*;
import java.nio.charset.StandardCharsets;
public final class DemoSeed {
    private static final Map<String,Object> DATA;
    static {try(var input=DemoSeed.class.getResourceAsStream("/demo-seed.json")) {
        DATA=Json.map(Json.decode(new String(Objects.requireNonNull(input).readAllBytes(),StandardCharsets.UTF_8)));
    }catch(Exception e){throw new ExceptionInInitializerError(e);}}
    private DemoSeed() {}
    @SuppressWarnings("unchecked") public static List<List<Object>> tuples(String key){return (List<List<Object>>)DATA.get(key);}
    public static Map<String,Object> mapping(String key){return Json.map(DATA.get(key));}
    public static List<Map<String,Object>> scenarios(){return Json.rows(DATA.get("scenarios"));}
}
