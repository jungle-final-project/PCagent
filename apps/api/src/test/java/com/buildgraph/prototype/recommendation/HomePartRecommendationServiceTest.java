package com.buildgraph.prototype.recommendation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.buildgraph.prototype.user.CurrentUserService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

@ExtendWith(MockitoExtension.class)
class HomePartRecommendationServiceTest {
    private static final CurrentUserService.CurrentUser USER = new CurrentUserService.CurrentUser(
            1L,
            "00000000-0000-4000-8000-000000001001",
            "user@example.com",
            "Demo User",
            "USER",
            null
    );

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Mock
    private RecommendationScoringClient scoringClient;

    @Mock
    private RecommendationModelRegistry modelRegistry;

    @Test
    void homePartsFallsBackAndKeepsCategoryDiversityWhenScorerFails() throws Exception {
        when(jdbcTemplate.queryForList(anyString())).thenReturn(List.of(
                part("gpu-1", "GPU", 900000, 95),
                part("gpu-2", "GPU", 700000, 90),
                part("cpu-1", "CPU", 600000, 93),
                part("ram-1", "RAM", 180000, 80),
                part("psu-1", "PSU", 210000, 78)
        ));
        when(scoringClient.payload(org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.eq("HOME_PARTS"), org.mockito.ArgumentMatchers.eq(false), org.mockito.ArgumentMatchers.anyList()))
                .thenReturn(Map.of("candidates", List.of()));
        when(scoringClient.score(org.mockito.ArgumentMatchers.anyMap())).thenThrow(new IllegalStateException("scorer down"));
        HomePartRecommendationService service = new HomePartRecommendationService(jdbcTemplate, scoringClient, modelRegistry, true);

        Map<String, Object> response = service.homeParts(USER, 4);

        assertThat(response.get("fallbackUsed")).isEqualTo(true);
        List<Map<String, Object>> items = castList(response.get("items"));
        assertThat(items).hasSize(4);
        assertThat(items.stream()
                .map(item -> castMap(item.get("part")).get("category"))
                .distinct()
                .count()).isEqualTo(4);
        verifyNoInteractions(modelRegistry);
    }

    private static Map<String, Object> part(String id, String category, int price, int score) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("internal_id", Math.abs(id.hashCode()));
        row.put("id", id);
        row.put("category", category);
        row.put("name", category + " part");
        row.put("manufacturer", "BuildGraph");
        row.put("price", price);
        row.put("status", "ACTIVE");
        row.put("attributes", Map.of("toolReady", true));
        row.put("benchmark_score", score);
        row.put("external_offer_image_url", "https://example.com/" + id + ".png");
        row.put("external_offer_url", "https://example.com/" + id);
        row.put("has_fps_coverage", "GPU".equals(category) || "CPU".equals(category));
        row.put("price_age_days", 1);
        return row;
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> castMap(Object value) {
        return (Map<String, Object>) value;
    }
}
