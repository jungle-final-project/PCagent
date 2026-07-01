package com.buildgraph.prototype.build;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.buildgraph.prototype.common.MockData;
import com.buildgraph.prototype.part.ToolCheckService;
import com.buildgraph.prototype.user.CurrentUserService;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.server.ResponseStatusException;

class BuildGraphServiceTest {
    private static final String USER_TOKEN = "Bearer jwt-user-token";

    private final JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
    private final ToolCheckService toolCheckService = mock(ToolCheckService.class);
    private final CurrentUserService currentUserService = mock(CurrentUserService.class);
    private final BuildGraphService buildGraphService = new BuildGraphService(jdbcTemplate, toolCheckService, currentUserService);

    @Test
    void aiBuildGraphShowsCoreDependenciesAndWarnsForPowerHeadroom() {
        stubPart("part-cpu", part("part-cpu", 101L, "CPU", "Ryzen 7", 420000, MockData.map("socket", "AM5", "tdpW", 120)));
        stubPart("part-board", part("part-board", 102L, "MOTHERBOARD", "B650 Board", 260000, MockData.map("socket", "AM5", "memoryType", "DDR5")));
        stubPart("part-ram", part("part-ram", 103L, "RAM", "DDR5 32GB", 140000, MockData.map("memoryType", "DDR5")));
        stubPart("part-gpu", part("part-gpu", 104L, "GPU", "RTX 5070", 890000, MockData.map("wattage", 250, "requiredSystemPowerW", 750, "lengthMm", 304)));
        stubPart("part-psu", part("part-psu", 105L, "PSU", "650W Bronze", 90000, MockData.map("capacityW", 650)));
        stubPart("part-case", part("part-case", 106L, "CASE", "Compact Case", 110000, MockData.map("maxGpuLengthMm", 320, "maxCpuCoolerHeightMm", 160)));
        stubPart("part-cooler", part("part-cooler", 107L, "COOLER", "AM5 Cooler", 80000, MockData.map("socketSupport", List.of("AM5"), "heightMm", 155)));
        when(toolCheckService.checkBuild(anyList(), eq(2_000_000))).thenReturn(List.of(
                tool("compatibility", "PASS", "CPU, 메인보드, RAM, 쿨러 기본 호환성이 맞습니다.",
                        MockData.map("socketMatched", true, "memoryTypeMatched", true, "coolerSocketMatched", true)),
                tool("power", "WARN", "PSU 정격 출력 여유가 낮습니다.",
                        MockData.map("requiredRatedCapacityW", 750, "psuRatedCapacityW", 650, "ratedHeadroomW", 80)),
                tool("size", "PASS", "GPU 길이와 쿨러 높이가 케이스 제약 안에 있습니다.",
                        MockData.map("gpuLengthMm", 304, "maxGpuLengthMm", 320, "coolerHeightMm", 155, "maxCpuCoolerHeightMm", 160)),
                tool("performance", "PASS", "요구 작업에 무리가 적은 조합입니다.", MockData.map("gpu", "RTX 5070", "cpu", "Ryzen 7")),
                tool("price", "PASS", "저장된 현재가 기준 예산 안에 들어옵니다.",
                        MockData.map("budget", 2000000, "totalPrice", 1990000, "priceDiff", -10000))
        ));

        Map<String, Object> graph = buildGraphService.resolve(USER_TOKEN, Map.of(
                "source", "AI_BUILD",
                "view", "FOCUSED",
                "budgetWon", 2_000_000,
                "items", List.of(
                        requestItem("part-cpu", "CPU"),
                        requestItem("part-board", "MOTHERBOARD"),
                        requestItem("part-ram", "RAM"),
                        requestItem("part-gpu", "GPU"),
                        requestItem("part-psu", "PSU"),
                        requestItem("part-case", "CASE"),
                        requestItem("part-cooler", "COOLER")
                ),
                "focus", Map.of("mode", "PART_IMPACT", "category", "GPU", "tool", "power")
        ));

        assertThat(graph.get("mode")).isEqualTo("PART_IMPACT");
        assertThat((String) graph.get("summary")).contains("GPU");
        List<Map<String, Object>> edges = castList(graph.get("edges"));
        assertThat(edges).anySatisfy(edge -> {
            assertThat(edge.get("id")).isEqualTo("edge-gpu-psu-power");
            assertThat(edge.get("status")).isEqualTo("WARN");
        });
        assertThat(edges).anySatisfy(edge -> assertThat(edge.get("id")).isEqualTo("edge-gpu-case-length"));
        assertThat(edges).anySatisfy(edge -> assertThat(edge.get("id")).isEqualTo("edge-cpu-board-socket"));
        List<Map<String, Object>> insights = castList(graph.get("insights"));
        assertThat(insights).anySatisfy(insight -> {
            assertThat(insight.get("title")).isEqualTo("파워 여유 확인");
            assertThat(insight.get("status")).isEqualTo("WARN");
        });
    }

    @Test
    void quoteDraftGraphReadsCurrentUserDraftWithoutClientItems() {
        when(currentUserService.requireUser(USER_TOKEN)).thenReturn(currentUser());
        when(jdbcTemplate.queryForList(anyString(), eq(1004L))).thenReturn(List.of(activeDraft()));
        when(jdbcTemplate.queryForList(anyString(), eq(700L))).thenReturn(List.of(
                draftItem("part-gpu", "GPU", "RTX 5070", 890000, MockData.map("wattage", 250, "requiredSystemPowerW", 750, "lengthMm", 304)),
                draftItem("part-psu", "PSU", "750W Gold", 150000, MockData.map("capacityW", 750)),
                draftItem("part-case", "CASE", "Airflow Case", 160000, MockData.map("maxGpuLengthMm", 360, "maxCpuCoolerHeightMm", 170))
        ));
        when(toolCheckService.checkBuild(anyList(), eq(1_200_000))).thenReturn(List.of(tool("price", "PASS", "확인되었습니다.", Map.of())));

        Map<String, Object> graph = buildGraphService.resolve(USER_TOKEN, Map.of(
                "source", "QUOTE_DRAFT_CURRENT",
                "view", "FOCUSED",
                "focus", Map.of("mode", "ISSUE_PATH")
        ));

        verify(currentUserService).requireUser(USER_TOKEN);
        List<Map<String, Object>> nodes = castList(graph.get("nodes"));
        assertThat(nodes).extracting(node -> node.get("id")).contains("part-GPU", "part-PSU", "part-CASE");
        assertThat(graph.get("mode")).isEqualTo("ISSUE_PATH");
    }

    @Test
    void aiBuildGraphRejectsUnknownPartIdBeforeToolCheck() {
        when(jdbcTemplate.queryForList(anyString(), eq("missing-part"))).thenReturn(List.of());

        assertThatThrownBy(() -> buildGraphService.resolve(USER_TOKEN, Map.of(
                "source", "AI_BUILD",
                "items", List.of(requestItem("missing-part", "GPU"))
        )))
                .isInstanceOf(ResponseStatusException.class)
                .extracting("statusCode")
                .isEqualTo(HttpStatus.NOT_FOUND);
    }

    private void stubPart(String publicId, Map<String, Object> row) {
        when(jdbcTemplate.queryForList(anyString(), eq(publicId))).thenReturn(List.of(row));
    }

    private static Map<String, Object> requestItem(String partId, String category) {
        return Map.of("partId", partId, "category", category, "quantity", 1);
    }

    private static Map<String, Object> part(String publicId, long internalId, String category, String name, int price, Map<String, Object> attributes) {
        return MockData.map(
                "internal_id", internalId,
                "id", publicId,
                "category", category,
                "name", name,
                "manufacturer", "BuildGraph",
                "price", price,
                "attributes", attributes
        );
    }

    private static Map<String, Object> activeDraft() {
        return Map.of(
                "internal_id", 700L,
                "id", "draft-public-id",
                "status", "ACTIVE",
                "name", "셀프 견적"
        );
    }

    private static Map<String, Object> draftItem(String publicId, String category, String name, int price, Map<String, Object> attributes) {
        return MockData.map(
                "internal_id", 100L,
                "part_id", publicId,
                "id", "draft-item-" + category,
                "category", category,
                "name", name,
                "manufacturer", "BuildGraph",
                "current_price", price,
                "quantity", 1,
                "attributes", attributes
        );
    }

    private static Map<String, Object> tool(String tool, String status, String summary, Map<String, Object> details) {
        return MockData.map(
                "tool", tool,
                "status", status,
                "confidence", "MEDIUM",
                "summary", summary,
                "details", details
        );
    }

    private CurrentUserService.CurrentUser currentUser() {
        return new CurrentUserService.CurrentUser(
                1004L,
                "00000000-0000-4000-8000-000000001004",
                "user@example.com",
                "Demo User",
                "USER",
                "2026-06-30T00:00:00Z"
        );
    }

    @SuppressWarnings("unchecked")
    private static List<Map<String, Object>> castList(Object value) {
        return (List<Map<String, Object>>) value;
    }
}
