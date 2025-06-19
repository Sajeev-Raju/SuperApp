package com.example.NearMeBKND.util;

import java.util.ArrayList;
import java.util.List;

public class QueryLogger {
    private static final ThreadLocal<List<String>> queries = ThreadLocal.withInitial(ArrayList::new);

    public static void log(String sql, long timeMs) {
        queries.get().add("SQL: " + sql + " | Time: " + timeMs + "ms");
    }

    public static List<String> getQueries() {
        return new ArrayList<>(queries.get());
    }

    public static void clear() {
        queries.remove();
    }
} 