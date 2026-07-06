package com.buildgraph.prototype.build;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;

final class BuildChatDataVersionReader {
    private static final List<TableVersionSpec> TABLES = List.of(
            new TableVersionSpec("parts_version", "parts", "coalesce(updated_at, created_at)", "deleted_at IS NULL"),
            new TableVersionSpec("benchmark_version", "benchmark_summaries", "created_at", "deleted_at IS NULL"),
            new TableVersionSpec("fps_version", "game_fps_benchmarks", "created_at", "deleted_at IS NULL"),
            new TableVersionSpec("rag_version", "rag_evidence", "created_at", "agent_session_id IS NULL"),
            new TableVersionSpec("alias_version", "part_alias_rules", "coalesce(updated_at, created_at)", "deleted_at IS NULL")
    );

    private BuildChatDataVersionReader() {
    }

    static Map<String, Object> read(JdbcTemplate jdbcTemplate) {
        Map<String, Object> versions = new LinkedHashMap<>();
        for (TableVersionSpec spec : TABLES) {
            versions.put(spec.key(), tableVersion(jdbcTemplate, spec));
        }
        return versions;
    }

    private static String tableVersion(JdbcTemplate jdbcTemplate, TableVersionSpec spec) {
        try {
            if (!tableExists(jdbcTemplate, spec.tableName())) {
                return "none";
            }
            String sql = "SELECT coalesce(max(" + spec.timestampExpression() + ")::text, 'none') "
                    + "FROM public." + spec.tableName() + " "
                    + "WHERE " + spec.whereClause();
            String result = jdbcTemplate.queryForObject(sql, String.class);
            return result == null || result.isBlank() ? "none" : result;
        } catch (Exception ignored) {
            return "none";
        }
    }

    private static boolean tableExists(JdbcTemplate jdbcTemplate, String tableName) {
        Boolean exists = jdbcTemplate.queryForObject(
                "SELECT to_regclass('public." + tableName + "') IS NOT NULL",
                Boolean.class
        );
        return Boolean.TRUE.equals(exists);
    }

    private record TableVersionSpec(String key, String tableName, String timestampExpression, String whereClause) {
    }
}
