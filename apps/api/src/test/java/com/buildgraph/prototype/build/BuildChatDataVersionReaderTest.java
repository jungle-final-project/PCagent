package com.buildgraph.prototype.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class BuildChatDataVersionReaderTest {
    @Test
    void missingAliasTableUsesNoneWithoutSelectingFromMissingRelation() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        stubTable(jdbcTemplate, "parts", true, "parts-v1");
        stubTable(jdbcTemplate, "benchmark_summaries", true, "benchmark-v1");
        stubTable(jdbcTemplate, "game_fps_benchmarks", true, "fps-v1");
        stubTable(jdbcTemplate, "rag_evidence", true, "rag-v1");
        stubTable(jdbcTemplate, "part_alias_rules", false, null);

        Map<String, Object> versions = BuildChatDataVersionReader.read(jdbcTemplate);

        assertThat(versions).containsEntry("alias_version", "none");
        verify(jdbcTemplate, never()).queryForObject(contains("FROM public.part_alias_rules "), org.mockito.ArgumentMatchers.eq(String.class));
    }

    @Test
    void versionQueryFailureFallsBackToNoneForThatVersionOnly() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        stubTable(jdbcTemplate, "parts", true, "parts-v1");
        stubTable(jdbcTemplate, "benchmark_summaries", true, "benchmark-v1");
        stubTable(jdbcTemplate, "game_fps_benchmarks", true, "fps-v1");
        stubTable(jdbcTemplate, "rag_evidence", true, "rag-v1");
        when(jdbcTemplate.queryForObject("SELECT to_regclass('public.part_alias_rules') IS NOT NULL", Boolean.class))
                .thenReturn(true);
        when(jdbcTemplate.queryForObject(contains("FROM public.part_alias_rules "), org.mockito.ArgumentMatchers.eq(String.class)))
                .thenThrow(new RuntimeException("column missing"));

        Map<String, Object> versions = BuildChatDataVersionReader.read(jdbcTemplate);

        assertThat(versions)
                .containsEntry("parts_version", "parts-v1")
                .containsEntry("alias_version", "none");
    }

    private static void stubTable(JdbcTemplate jdbcTemplate, String tableName, boolean exists, String version) {
        when(jdbcTemplate.queryForObject("SELECT to_regclass('public." + tableName + "') IS NOT NULL", Boolean.class))
                .thenReturn(exists);
        if (exists) {
            when(jdbcTemplate.queryForObject(contains("FROM public." + tableName + " "), org.mockito.ArgumentMatchers.eq(String.class)))
                    .thenReturn(version);
        }
    }
}
