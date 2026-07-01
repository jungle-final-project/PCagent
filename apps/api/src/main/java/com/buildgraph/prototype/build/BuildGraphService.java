package com.buildgraph.prototype.build;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.buildgraph.prototype.part.ToolBuildPart;
import com.buildgraph.prototype.part.ToolCheckService;
import com.buildgraph.prototype.user.CurrentUserService;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuildGraphService {
    private static final Set<String> CATEGORIES = Set.of("CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE", "COOLER");
    private final JdbcTemplate jdbcTemplate;
    private final ToolCheckService toolCheckService;
    private final CurrentUserService currentUserService;

    public BuildGraphService(JdbcTemplate jdbcTemplate, ToolCheckService toolCheckService, CurrentUserService currentUserService) {
        this.jdbcTemplate = jdbcTemplate;
        this.toolCheckService = toolCheckService;
        this.currentUserService = currentUserService;
    }

    public Map<String, Object> resolve(String authorization, Map<String, Object> request) {
        Map<String, Object> body = request == null ? Map.of() : request;
        String source = firstText(text(body.get("source")), "AI_BUILD").toUpperCase(Locale.ROOT);
        String view = firstText(text(body.get("view")), "FOCUSED").toUpperCase(Locale.ROOT);
        Map<String, Object> focus = objectMap(body.get("focus"));
        String mode = normalizedMode(text(focus.get("mode")));
        List<ToolBuildPart> parts = switch (source) {
            case "AI_BUILD" -> aiBuildParts(body);
            case "QUOTE_DRAFT_CURRENT" -> currentQuoteDraftParts(authorization);
            default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 그래프 source입니다.");
        };
        int total = total(parts);
        int budget = firstNumber(body.get("budgetWon"), total);
        List<Map<String, Object>> toolResults = parts.isEmpty() ? List.of() : toolCheckService.checkBuild(parts, budget);
        GraphDraft draft = buildGraph(parts, toolResults, mode, view, focus, budget, total);
        return MockData.map(
                "mode", mode,
                "summary", draft.summary(),
                "nodes", draft.nodes(),
                "edges", draft.edges(),
                "focusNodeIds", draft.focusNodeIds(),
                "insights", draft.insights(),
                "toolResults", toolResults
        );
    }

    private List<ToolBuildPart> aiBuildParts(Map<String, Object> body) {
        List<Map<String, Object>> items = objectMaps(body.get("items"));
        if (items.isEmpty()) {
            return List.of();
        }
        List<ToolBuildPart> parts = new ArrayList<>();
        for (Map<String, Object> item : items) {
            String partId = text(item.get("partId"));
            if (partId == null || partId.isBlank()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "partId가 필요합니다.");
            }
            ToolBuildPart part = partByPublicId(partId);
            String requestedCategory = normalizeCategory(text(item.get("category")));
            if (requestedCategory != null && !requestedCategory.equals(part.category())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "partId와 category가 일치하지 않습니다.");
            }
            parts.add(part);
        }
        return parts;
    }

    private ToolBuildPart partByPublicId(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               category,
                               name,
                               manufacturer,
                               price,
                               attributes
                        FROM parts
                        WHERE public_id::text = ?
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """, publicId)
                .stream()
                .findFirst()
                .map(this::part)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "활성 부품을 찾을 수 없습니다."));
    }

    private List<ToolBuildPart> currentQuoteDraftParts(String authorization) {
        CurrentUserService.CurrentUser user = currentUserService.requireUser(authorization);
        List<Map<String, Object>> drafts = jdbcTemplate.queryForList("""
                SELECT id AS internal_id,
                       public_id::text AS id,
                       status,
                       name
                FROM quote_drafts
                WHERE user_id = ?
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                ORDER BY updated_at DESC, id DESC
                LIMIT 1
                """, user.internalId());
        if (drafts.isEmpty()) {
            return List.of();
        }
        Long draftId = longValue(drafts.get(0).get("internal_id"));
        return jdbcTemplate.queryForList("""
                        SELECT p.id AS internal_id,
                               p.public_id::text AS part_id,
                               qdi.public_id::text AS id,
                               p.category,
                               p.name,
                               p.manufacturer,
                               p.price AS current_price,
                               qdi.quantity,
                               p.attributes
                        FROM quote_draft_items qdi
                        JOIN parts p ON p.id = qdi.part_id
                        WHERE qdi.quote_draft_id = ?
                          AND qdi.deleted_at IS NULL
                          AND p.deleted_at IS NULL
                        ORDER BY qdi.id
                        """, draftId)
                .stream()
                .map(row -> new ToolBuildPart(
                        longValue(row.get("internal_id")),
                        DbValueMapper.string(row, "part_id"),
                        DbValueMapper.string(row, "category"),
                        DbValueMapper.string(row, "name"),
                        DbValueMapper.string(row, "manufacturer"),
                        numberValue(row.get("current_price")),
                        objectMap(row.get("attributes"))
                ))
                .toList();
    }

    private GraphDraft buildGraph(
            List<ToolBuildPart> parts,
            List<Map<String, Object>> toolResults,
            String mode,
            String view,
            Map<String, Object> focus,
            int budget,
            int total
    ) {
        Map<String, ToolBuildPart> byCategory = parts.stream()
                .filter(part -> part.category() != null)
                .collect(Collectors.toMap(part -> part.category().toUpperCase(Locale.ROOT), part -> part, (left, right) -> left, LinkedHashMap::new));
        Map<String, Map<String, Object>> toolByName = toolResults.stream()
                .filter(result -> result.get("tool") != null)
                .collect(Collectors.toMap(result -> text(result.get("tool")), result -> result, (left, right) -> left, LinkedHashMap::new));
        List<Map<String, Object>> nodes = new ArrayList<>();
        for (ToolBuildPart part : parts) {
            nodes.add(node(part));
        }
        addConstraintNodes(nodes, byCategory, toolByName, budget, total);

        List<Map<String, Object>> edges = new ArrayList<>();
        addEdgeIfPossible(edges, byCategory, "CPU", "MOTHERBOARD", "edge-cpu-board-socket", "REQUIRES", toolStatus(toolByName, "compatibility"), "소켓", socketSummary(byCategory));
        addEdgeIfPossible(edges, byCategory, "MOTHERBOARD", "RAM", "edge-board-ram-memory", "REQUIRES", toolStatus(toolByName, "compatibility"), "DDR 규격", memorySummary(byCategory));
        addEdgeIfPossible(edges, byCategory, "CPU", "COOLER", "edge-cpu-cooler-socket", "REQUIRES", toolStatus(toolByName, "compatibility"), "쿨러 장착", coolerSummary(byCategory));
        addEdgeIfPossible(edges, byCategory, "GPU", "PSU", "edge-gpu-psu-power", "AFFECTS", toolStatus(toolByName, "power"), "전력 여유", powerSummary(toolByName));
        addEdgeIfPossible(edges, byCategory, "GPU", "CASE", "edge-gpu-case-length", "REQUIRES", toolStatus(toolByName, "size"), "장착 길이", gpuLengthSummary(toolByName));
        addEdgeIfPossible(edges, byCategory, "COOLER", "CASE", "edge-cooler-case-height", "REQUIRES", toolStatus(toolByName, "size"), "쿨러 높이", coolerHeightSummary(toolByName));
        addEdgeIfPossible(edges, byCategory, "CPU", "GPU", "edge-cpu-gpu-performance", "AFFECTS", toolStatus(toolByName, "performance"), "작업 성능", toolSummary(toolByName, "performance", "CPU와 GPU 조합으로 작업 적합도를 확인합니다."));
        if (!parts.isEmpty()) {
            edges.add(edge("edge-budget-total-price", "constraint-budget", "constraint-total-price", "AFFECTS", toolStatus(toolByName, "price"), "예산", priceSummary(toolByName, budget, total)));
        }

        List<Map<String, Object>> insights = insights(toolByName, byCategory, budget, total);
        List<String> focusNodeIds = focusNodeIds(edges, focus, insights);
        String summary = summary(mode, focus, toolByName, byCategory, parts);
        return new GraphDraft(summary, nodes, edges, focusNodeIds, insights);
    }

    private void addConstraintNodes(List<Map<String, Object>> nodes, Map<String, ToolBuildPart> byCategory, Map<String, Map<String, Object>> toolByName, int budget, int total) {
        if (byCategory.containsKey("GPU") || byCategory.containsKey("PSU")) {
            nodes.add(constraintNode("constraint-power", "PSU", "전력 여유", toolStatus(toolByName, "power"), powerSummary(toolByName)));
        }
        if (byCategory.containsKey("GPU") || byCategory.containsKey("CASE") || byCategory.containsKey("COOLER")) {
            nodes.add(constraintNode("constraint-size", "CASE", "장착 규격", toolStatus(toolByName, "size"), toolSummary(toolByName, "size", "케이스와 부품 치수 제약을 확인합니다.")));
        }
        if (byCategory.containsKey("CPU") || byCategory.containsKey("MOTHERBOARD") || byCategory.containsKey("RAM") || byCategory.containsKey("COOLER")) {
            nodes.add(constraintNode("constraint-compatibility", "MOTHERBOARD", "기본 호환성", toolStatus(toolByName, "compatibility"), toolSummary(toolByName, "compatibility", "소켓과 메모리 규격을 확인합니다.")));
        }
        if (!byCategory.isEmpty()) {
            nodes.add(constraintNode("constraint-budget", "PRICE", "예산", toolStatus(toolByName, "price"), budget > 0 ? formatWon(budget) : "예산 미지정"));
            nodes.add(constraintNode("constraint-total-price", "PRICE", "총액", toolStatus(toolByName, "price"), formatWon(total)));
        }
    }

    private Map<String, Object> node(ToolBuildPart part) {
        return MockData.map(
                "id", "part-" + part.category(),
                "partId", part.publicId(),
                "type", "PART",
                "category", part.category(),
                "label", part.name(),
                "status", "PASS",
                "detail", part.manufacturer() == null ? formatWon(firstNumber(part.price(), 0)) : part.manufacturer() + " · " + formatWon(firstNumber(part.price(), 0)),
                "price", firstNumber(part.price(), 0)
        );
    }

    private static Map<String, Object> constraintNode(String id, String category, String label, String status, String detail) {
        return MockData.map(
                "id", id,
                "type", "CONSTRAINT",
                "category", category,
                "label", label,
                "status", status,
                "detail", detail
        );
    }

    private static void addEdgeIfPossible(
            List<Map<String, Object>> edges,
            Map<String, ToolBuildPart> byCategory,
            String sourceCategory,
            String targetCategory,
            String id,
            String type,
            String status,
            String label,
            String summary
    ) {
        if (!byCategory.containsKey(sourceCategory) || !byCategory.containsKey(targetCategory)) {
            return;
        }
        edges.add(edge(id, "part-" + sourceCategory, "part-" + targetCategory, type, status, label, summary));
    }

    private static Map<String, Object> edge(String id, String source, String target, String type, String status, String label, String summary) {
        return MockData.map(
                "id", id,
                "source", source,
                "target", target,
                "type", type,
                "status", status,
                "label", label,
                "summary", summary
        );
    }

    private List<Map<String, Object>> insights(Map<String, Map<String, Object>> toolByName, Map<String, ToolBuildPart> byCategory, int budget, int total) {
        List<Map<String, Object>> items = new ArrayList<>();
        addToolInsight(items, toolByName, "compatibility", "호환성 확인", List.of("part-CPU", "part-MOTHERBOARD", "part-RAM", "part-COOLER"));
        addToolInsight(items, toolByName, "power", "파워 여유 확인", List.of("part-GPU", "part-PSU"));
        addToolInsight(items, toolByName, "size", "장착 규격 확인", List.of("part-GPU", "part-CASE", "part-COOLER"));
        addToolInsight(items, toolByName, "performance", "성능 균형 확인", List.of("part-CPU", "part-GPU"));
        addToolInsight(items, toolByName, "price", budget > 0 && total > budget ? "예산 초과 확인" : "예산 범위 확인", List.of("constraint-budget", "constraint-total-price"));
        if (items.isEmpty() && !byCategory.isEmpty()) {
            items.add(MockData.map(
                    "id", "insight-ready",
                    "status", "PASS",
                    "title", "관계 분석 준비 완료",
                    "description", "선택한 부품 간 핵심 의존성을 표시했습니다.",
                    "relatedNodeIds", byCategory.keySet().stream().map(category -> "part-" + category).toList()
            ));
        }
        return items;
    }

    private static void addToolInsight(List<Map<String, Object>> insights, Map<String, Map<String, Object>> toolByName, String tool, String title, List<String> nodeIds) {
        Map<String, Object> result = toolByName.get(tool);
        if (result == null) {
            return;
        }
        String status = status(result);
        if ("PASS".equals(status) && insights.stream().anyMatch(insight -> !"PASS".equals(insight.get("status")))) {
            return;
        }
        insights.add(MockData.map(
                "id", "insight-" + tool,
                "status", status,
                "title", title,
                "description", text(result.get("summary")),
                "relatedNodeIds", nodeIds
        ));
    }

    private static List<String> focusNodeIds(List<Map<String, Object>> edges, Map<String, Object> focus, List<Map<String, Object>> insights) {
        String category = normalizeCategory(text(focus.get("category")));
        if (category != null) {
            String nodeId = "part-" + category;
            return edges.stream()
                    .filter(edge -> Objects.equals(edge.get("source"), nodeId) || Objects.equals(edge.get("target"), nodeId))
                    .flatMap(edge -> List.of(text(edge.get("source")), text(edge.get("target"))).stream())
                    .filter(Objects::nonNull)
                    .distinct()
                    .toList();
        }
        return insights.stream()
                .findFirst()
                .map(insight -> stringList(insight.get("relatedNodeIds")))
                .orElse(List.of());
    }

    private static String summary(String mode, Map<String, Object> focus, Map<String, Map<String, Object>> toolByName, Map<String, ToolBuildPart> byCategory, List<ToolBuildPart> parts) {
        String category = normalizeCategory(text(focus.get("category")));
        if ("PART_IMPACT".equals(mode) && category != null) {
            return category + " 선택으로 영향을 받는 부품과 제약을 확인했습니다.";
        }
        if ("ISSUE_PATH".equals(mode)) {
            return "현재 견적에서 주의가 필요한 관계를 먼저 표시했습니다.";
        }
        if (parts.isEmpty()) {
            return "부품을 담으면 관계 그래프가 자동으로 구성됩니다.";
        }
        long warningCount = toolByName.values().stream().filter(result -> !"PASS".equals(status(result))).count();
        if (warningCount > 0) {
            return "추천 조합에서 확인이 필요한 관계 " + warningCount + "개를 표시했습니다.";
        }
        return "선택한 조합의 핵심 호환성, 전력, 규격 관계를 확인했습니다.";
    }

    private ToolBuildPart part(Map<String, Object> row) {
        return new ToolBuildPart(
                longValue(row.get("internal_id")),
                firstText(DbValueMapper.string(row, "id"), DbValueMapper.string(row, "part_id")),
                DbValueMapper.string(row, "category"),
                DbValueMapper.string(row, "name"),
                DbValueMapper.string(row, "manufacturer"),
                firstNumber(row.get("price"), firstNumber(row.get("current_price"), 0)),
                objectMap(row.get("attributes"))
        );
    }

    private static String socketSummary(Map<String, ToolBuildPart> byCategory) {
        return "CPU " + attr(byCategory.get("CPU"), "socket") + " 소켓과 메인보드 " + attr(byCategory.get("MOTHERBOARD"), "socket") + " 소켓을 비교합니다.";
    }

    private static String memorySummary(Map<String, ToolBuildPart> byCategory) {
        return "RAM " + attr(byCategory.get("RAM"), "memoryType") + " 규격과 메인보드 " + attr(byCategory.get("MOTHERBOARD"), "memoryType") + " 지원 규격을 비교합니다.";
    }

    private static String coolerSummary(Map<String, ToolBuildPart> byCategory) {
        return "CPU 소켓 " + attr(byCategory.get("CPU"), "socket") + "을 쿨러 장착 지원 목록과 비교합니다.";
    }

    private static String powerSummary(Map<String, Map<String, Object>> toolByName) {
        Map<String, Object> result = toolByName.get("power");
        if (result == null) {
            return "GPU 소비전력과 PSU 정격 출력을 함께 확인합니다.";
        }
        Map<String, Object> details = objectMap(result.get("details"));
        Integer required = numberValue(details.get("requiredRatedCapacityW"));
        Integer capacity = numberValue(details.get("psuRatedCapacityW"));
        if (required != null && capacity != null) {
            return "권장 " + required + "W / 현재 " + capacity + "W 기준으로 여유를 판단합니다.";
        }
        return toolSummary(toolByName, "power", "GPU 소비전력과 PSU 정격 출력을 함께 확인합니다.");
    }

    private static String gpuLengthSummary(Map<String, Map<String, Object>> toolByName) {
        Map<String, Object> details = objectMap(toolByName.getOrDefault("size", Map.of()).get("details"));
        Integer gpuLength = numberValue(details.get("gpuLengthMm"));
        Integer maxGpuLength = numberValue(details.get("maxGpuLengthMm"));
        if (gpuLength != null && maxGpuLength != null) {
            return "GPU " + gpuLength + "mm / 케이스 허용 " + maxGpuLength + "mm를 비교합니다.";
        }
        return "GPU 길이가 케이스 허용 길이 안에 있는지 확인합니다.";
    }

    private static String coolerHeightSummary(Map<String, Map<String, Object>> toolByName) {
        Map<String, Object> details = objectMap(toolByName.getOrDefault("size", Map.of()).get("details"));
        Integer coolerHeight = numberValue(details.get("coolerHeightMm"));
        Integer maxCoolerHeight = numberValue(details.get("maxCpuCoolerHeightMm"));
        if (coolerHeight != null && maxCoolerHeight != null) {
            return "쿨러 " + coolerHeight + "mm / 케이스 허용 " + maxCoolerHeight + "mm를 비교합니다.";
        }
        return "CPU 쿨러 높이가 케이스 제한 안에 있는지 확인합니다.";
    }

    private static String priceSummary(Map<String, Map<String, Object>> toolByName, int budget, int total) {
        Map<String, Object> result = toolByName.get("price");
        if (result != null) {
            return toolSummary(toolByName, "price", "예산과 총액을 비교합니다.");
        }
        return "예산 " + formatWon(budget) + " / 총액 " + formatWon(total) + " 기준입니다.";
    }

    private static String toolSummary(Map<String, Map<String, Object>> toolByName, String tool, String fallback) {
        Map<String, Object> result = toolByName.get(tool);
        if (result == null) {
            return fallback;
        }
        return firstText(text(result.get("summary")), fallback);
    }

    private static String toolStatus(Map<String, Map<String, Object>> toolByName, String tool) {
        return status(toolByName.get(tool));
    }

    private static String status(Map<String, Object> result) {
        if (result == null) {
            return "WARN";
        }
        String value = text(result.get("status"));
        return value == null ? "WARN" : value.toUpperCase(Locale.ROOT);
    }

    private static String attr(ToolBuildPart part, String key) {
        if (part == null) {
            return "미확인";
        }
        Object value = part.attributes().get(key);
        return value == null ? "미확인" : String.valueOf(value);
    }

    private static int total(List<ToolBuildPart> parts) {
        return parts.stream().mapToInt(part -> firstNumber(part.price(), 0)).sum();
    }

    private static String normalizedMode(String value) {
        if (value == null || value.isBlank()) {
            return "BUILD_OVERVIEW";
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        return switch (normalized) {
            case "BUILD_OVERVIEW", "PART_IMPACT", "ISSUE_PATH", "DRAFT_ACTION" -> normalized;
            default -> "BUILD_OVERVIEW";
        };
    }

    private static String normalizeCategory(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.toUpperCase(Locale.ROOT);
        return CATEGORIES.contains(normalized) ? normalized : null;
    }

    private static List<Map<String, Object>> objectMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream()
                .filter(Map.class::isInstance)
                .map(item -> objectMap(item))
                .toList();
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> {
                if (key != null) {
                    result.put(String.valueOf(key), mapValue);
                }
            });
            return result;
        }
        if (value == null) {
            return Map.of();
        }
        return (Map<String, Object>) DbValueMapper.json(Map.of("json", value), "json", Map.of());
    }

    private static List<String> stringList(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().map(String::valueOf).toList();
    }

    private static String text(Object value) {
        return value == null ? null : String.valueOf(value);
    }

    private static String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static int firstNumber(Object value, int fallback) {
        Integer number = numberValue(value);
        return number == null ? fallback : number;
    }

    private static Integer numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return (int) Math.round(Double.parseDouble(value.toString()));
        } catch (NumberFormatException ignored) {
            return null;
        }
    }

    private static Long longValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value == null) {
            return null;
        }
        return Long.valueOf(value.toString());
    }

    private static String formatWon(int value) {
        return String.format(Locale.KOREA, "%,d원", value);
    }

    private record GraphDraft(
            String summary,
            List<Map<String, Object>> nodes,
            List<Map<String, Object>> edges,
            List<String> focusNodeIds,
            List<Map<String, Object>> insights
    ) {
    }
}
