package com.buildgraph.prototype.build;

import com.buildgraph.prototype.agent.AgentRunProfile;
import com.buildgraph.prototype.agent.AgentRunProfiles;
import com.buildgraph.prototype.agent.AgentRunner;
import com.buildgraph.prototype.agent.AgentSessionRoot;
import com.buildgraph.prototype.agent.AgentSessionRootType;
import com.buildgraph.prototype.agent.AgentStatus;
import com.buildgraph.prototype.agent.AgentTraceService;
import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class BuildQueryService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Pattern BUDGET_MANWON = Pattern.compile("([0-9]{2,4})\\s*만\\s*원?");
    private static final Pattern BUDGET_NUMBER = Pattern.compile("([0-9][0-9,]{5,})\\s*원?");
    private static final List<String> BUILD_CATEGORIES = List.of(
            "CPU", "MOTHERBOARD", "RAM", "GPU", "STORAGE", "PSU", "CASE", "COOLER"
    );
    private static final List<BuildPlan> BUILD_PLANS = List.of(
            new BuildPlan("가성비형 추천 Build", "예산 안에서 핵심 성능을 먼저 확보", 0, 0.78, "MEDIUM"),
            new BuildPlan("균형형 추천 Build", "게임, 개발, 안정성을 균형 있게 반영", 1, 0.96, "HIGH"),
            new BuildPlan("고성능형 추천 Build", "성능 우선 조건과 업그레이드 여유 확보", 2, 1.14, "MEDIUM")
    );

    private final JdbcTemplate jdbcTemplate;
    private final AgentTraceService agentTraceService;
    private final AgentRunner agentRunner;

    public BuildQueryService(JdbcTemplate jdbcTemplate, AgentTraceService agentTraceService, AgentRunner agentRunner) {
        this.jdbcTemplate = jdbcTemplate;
        this.agentTraceService = agentTraceService;
        this.agentRunner = agentRunner;
    }

    public Map<String, Object> parse(Map<String, Object> request) {
        Map<String, Object> body = request == null ? Map.of() : request;
        String message = text(body.get("message"));
        if (message == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "자연어 요구사항은 필수입니다.");
        }

        Integer budget = numberValue(body.get("budget"));
        if (budget == null) {
            budget = inferBudget(message);
        }
        List<String> usageTags = usageTags(body.get("usageTags"), message);
        String resolution = firstText(text(body.get("resolution")), inferResolution(message));
        List<String> preferredVendors = preferredVendors(body.get("preferredVendors"), message);
        String priority = text(body.get("priority"));
        List<String> mustHave = mustHave(message);

        Map<String, Object> parsedContext = MockData.map(
                "usageTags", usageTags,
                "budget", budget,
                "resolution", resolution,
                "preferredVendors", preferredVendors,
                "priority", priority,
                "mustHave", mustHave,
                "confidence", MockData.map(
                        "usageTags", usageTags.isEmpty() ? "LOW" : "HIGH",
                        "budget", budget == null ? "LOW" : "HIGH",
                        "resolution", resolution == null ? "LOW" : "MEDIUM",
                        "preferredVendors", preferredVendors.isEmpty() ? "LOW" : "MEDIUM"
                )
        );
        String id = jdbcTemplate.queryForObject("""
                INSERT INTO requirements (user_id, raw_message, budget, usage_tags, parsed_context)
                VALUES (
                  (SELECT id FROM users WHERE email = 'user@example.com'),
                  ?,
                  ?,
                  string_to_array(?, ','),
                  ?::jsonb
                )
                RETURNING public_id::text
                """, String.class, message, budget, String.join(",", usageTags), json(parsedContext));

        return MockData.map(
                "id", id,
                "rawMessage", message,
                "budget", budget,
                "usageTags", usageTags,
                "parsedContext", parsedContext,
                "questions", questions(parsedContext)
        );
    }

    public Map<String, Object> recommendations(Map<String, Object> request) {
        String requirementId = text(request == null ? null : request.get("requirementId"));
        if (requirementId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "requirementId가 필요합니다.");
        }
        RequirementRow requirement = requirement(requirementId);
        Map<String, Object> answers = objectMap(request == null ? null : request.get("answers"));
        int budget = effectiveBudget(requirement, answers);
        List<String> createdBuildIds = new ArrayList<>();
        for (BuildPlan plan : BUILD_PLANS) {
            List<PartCandidate> parts = selectBuildParts(requirement, answers, plan, budget);
            List<Map<String, Object>> toolResults = evaluateTools(parts, budget);
            List<Map<String, Object>> warnings = warningsFor(toolResults, total(parts), budget);
            createdBuildIds.add(insertBuild(requirement.internalId(), plan, parts, warnings));
        }

        String agentSessionId = runAgent(new AgentSessionRoot(AgentSessionRootType.REQUIREMENT, requirement.publicId()));
        List<Map<String, Object>> recommendations = createdBuildIds.stream()
                .map(this::buildDetail)
                .map(build -> with(build, "agentSessionId", agentSessionId))
                .toList();
        return MockData.map(
                "agentSessionId", agentSessionId,
                "recommendations", recommendations,
                "warnings", combinedWarnings(recommendations),
                "evidenceIds", agentSessionId == null ? List.of() : evidenceIdsBySession(agentSessionId),
                "toolResults", agentSessionId == null ? List.of() : toolResultsBySession(agentSessionId)
        );
    }

    public List<Map<String, Object>> builds() {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, name, total_price, confidence, warnings, created_at
                        FROM builds
                        ORDER BY created_at DESC, id DESC
                        LIMIT 30
                        """)
                .stream()
                .map(this::buildSummary)
                .toList();
    }

    public Map<String, Object> buildDetail(String id) {
        Map<String, Object> row = buildRow(id);
        Map<String, Object> summary = buildSummary(row);
        return MockData.map(
                "id", summary.get("id"),
                "name", summary.get("name"),
                "recommendedFor", summary.get("recommendedFor"),
                "summary", summary.get("summary"),
                "totalPrice", summary.get("totalPrice"),
                "confidence", summary.get("confidence"),
                "items", summary.get("items"),
                "warnings", summary.get("warnings"),
                "evidenceIds", summary.get("evidenceIds"),
                "agentSessionId", summary.get("agentSessionId"),
                "agentSummary", summary.get("agentSummary"),
                "changeableCategories", summary.get("changeableCategories"),
                "createdAt", summary.get("createdAt"),
                "toolResults", summary.get("toolResults")
        );
    }

    public Map<String, Object> changePart(String id, Map<String, Object> request) {
        String category = normalizeCategory(text(request == null ? null : request.get("category")));
        String selectedPartId = text(request == null ? null : request.get("partId"));
        if (selectedPartId == null) {
            selectedPartId = defaultPartId(category);
        }
        if (selectedPartId == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경할 부품을 찾을 수 없습니다.");
        }

        Map<String, Object> beforeBuild = buildDetail(id);
        List<PartCandidate> beforeParts = buildPartCandidates(id);
        PartCandidate selectedPart = partByPublicId(selectedPartId);
        List<PartCandidate> afterParts = replaceCategory(beforeParts, category, selectedPart);
        int beforeTotal = total(beforeParts);
        int afterTotal = total(afterParts);
        List<Map<String, Object>> toolResults = evaluateTools(afterParts, afterTotal);
        String agentSessionId = runAgent(new AgentSessionRoot(AgentSessionRootType.BUILD, id));
        Map<String, Object> agentSession = agentSessionId == null ? Map.of() : agentSession(agentSessionId);

        return MockData.map(
                "buildId", id,
                "category", category,
                "previousPartId", previousPartId(beforeParts, category),
                "selectedPartId", selectedPart.publicId(),
                "totalPrice", afterTotal,
                "diff", MockData.map("price", afterTotal - beforeTotal),
                "beforeBuild", beforeBuild,
                "afterBuild", MockData.map(
                        "id", id,
                        "name", beforeBuild.get("name"),
                        "totalPrice", afterTotal,
                        "items", afterParts.stream().map(this::partItem).toList()
                ),
                "diffRows", diffRows(beforeParts, afterParts, category, beforeTotal, afterTotal),
                "toolResults", toolResults,
                "agentSessionId", agentSessionId,
                "agentSummary", agentSession.get("summary"),
                "warnings", warningsFor(toolResults, afterTotal, afterTotal)
        );
    }

    private RequirementRow requirement(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               raw_message,
                               budget,
                               coalesce(array_to_string(usage_tags, ','), '') AS usage_tags,
                               parsed_context
                        FROM requirements
                        WHERE public_id = ?::uuid
                        """, publicId)
                .stream()
                .findFirst()
                .map(row -> new RequirementRow(
                        numberLong(row.get("internal_id")),
                        DbValueMapper.string(row, "id"),
                        DbValueMapper.string(row, "raw_message"),
                        DbValueMapper.integer(row, "budget"),
                        csv(DbValueMapper.string(row, "usage_tags")),
                        objectMap(DbValueMapper.json(row, "parsed_context", Map.of()))
                ))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "요구사항을 찾을 수 없습니다."));
    }

    private String insertBuild(Long requirementInternalId, BuildPlan plan, List<PartCandidate> parts, List<Map<String, Object>> warnings) {
        int totalPrice = total(parts);
        Map<String, Object> row = jdbcTemplate.queryForMap("""
                INSERT INTO builds (requirement_id, name, total_price, confidence, warnings)
                VALUES (?, ?, ?, ?, ?::jsonb)
                RETURNING id, public_id::text AS public_id
                """, requirementInternalId, plan.name(), totalPrice, plan.confidence(), json(warnings));
        Long buildInternalId = numberLong(row.get("id"));
        for (PartCandidate part : parts) {
            jdbcTemplate.update("""
                    INSERT INTO build_items (build_id, part_id, category, price)
                    VALUES (?, ?, ?, ?)
                    """, buildInternalId, part.internalId(), part.category(), part.price());
        }
        return DbValueMapper.string(row, "public_id");
    }

    private List<PartCandidate> selectBuildParts(
            RequirementRow requirement,
            Map<String, Object> answers,
            BuildPlan plan,
            int budget
    ) {
        List<PartCandidate> cpuCandidates = partCandidates("CPU");
        PartCandidate cpu = chooseByTarget(cpuCandidates, target(budget, plan, 0.17));
        String socket = stringAttr(cpu, "socket");
        List<PartCandidate> motherboardCandidates = filter(partCandidates("MOTHERBOARD"),
                part -> socket == null || socket.equalsIgnoreCase(stringAttr(part, "socket")));
        PartCandidate motherboard = chooseByTarget(orFallback(motherboardCandidates, partCandidates("MOTHERBOARD")), target(budget, plan, 0.11));
        String memoryType = firstText(stringAttr(motherboard, "memoryType"), "DDR5");
        List<PartCandidate> ramCandidates = filter(partCandidates("RAM"),
                part -> memoryType.equalsIgnoreCase(firstText(stringAttr(part, "memoryType"), memoryType)));
        PartCandidate ram = chooseByTarget(orFallback(ramCandidates, partCandidates("RAM")), target(budget, plan, 0.07));
        PartCandidate gpu = chooseByTarget(partCandidates("GPU"), target(budget, plan, 0.39));
        PartCandidate storage = chooseByTarget(partCandidates("STORAGE"), target(budget, plan, 0.07));
        int recommendedPsu = Math.max(intAttr(gpu, "requiredSystemPowerW", 650), estimatedWattage(List.of(cpu, gpu)) + 180);
        List<PartCandidate> psuCandidates = filter(partCandidates("PSU"), part -> intAttr(part, "capacityW", 0) >= recommendedPsu);
        PartCandidate psu = chooseByTarget(orFallback(psuCandidates, partCandidates("PSU")), target(budget, plan, 0.08));
        int gpuLength = intAttr(gpu, "lengthMm", 0);
        List<PartCandidate> caseCandidates = filter(partCandidates("CASE"), part -> intAttr(part, "maxGpuLengthMm", 0) >= gpuLength + 20);
        PartCandidate pcCase = chooseByTarget(orFallback(caseCandidates, partCandidates("CASE")), target(budget, plan, 0.06));
        int cpuTdp = intAttr(cpu, "tdpW", intAttr(cpu, "wattage", 120));
        List<PartCandidate> coolerCandidates = filter(partCandidates("COOLER"), part ->
                intAttr(part, "tdpW", 0) >= cpuTdp
                        && socketSupported(part, socket)
                        && intAttr(part, "heightMm", intAttr(part, "coolerHeightMm", 0)) <= intAttr(pcCase, "maxCpuCoolerHeightMm", 190)
        );
        PartCandidate cooler = chooseByTarget(orFallback(coolerCandidates, partCandidates("COOLER")), target(budget, plan, 0.05));
        return List.of(cpu, motherboard, ram, gpu, storage, psu, pcCase, cooler);
    }

    private List<Map<String, Object>> evaluateTools(List<PartCandidate> parts, int budget) {
        Map<String, PartCandidate> byCategory = byCategory(parts);
        PartCandidate cpu = byCategory.get("CPU");
        PartCandidate motherboard = byCategory.get("MOTHERBOARD");
        PartCandidate ram = byCategory.get("RAM");
        PartCandidate gpu = byCategory.get("GPU");
        PartCandidate psu = byCategory.get("PSU");
        PartCandidate pcCase = byCategory.get("CASE");
        PartCandidate cooler = byCategory.get("COOLER");
        int total = total(parts);

        boolean socketMatched = same(stringAttr(cpu, "socket"), stringAttr(motherboard, "socket"));
        boolean memoryMatched = same(firstText(stringAttr(ram, "memoryType"), "DDR5"), firstText(stringAttr(motherboard, "memoryType"), "DDR5"));
        boolean coolerMatched = socketSupported(cooler, stringAttr(cpu, "socket"));

        int estimatedWattage = estimatedWattage(parts);
        int psuCapacity = intAttr(psu, "capacityW", 0);
        int requiredPower = Math.max(intAttr(gpu, "requiredSystemPowerW", 0), estimatedWattage + 120);
        int headroom = psuCapacity - estimatedWattage;

        int gpuLength = intAttr(gpu, "lengthMm", 0);
        int maxGpuLength = intAttr(pcCase, "maxGpuLengthMm", 0);
        int coolerHeight = intAttr(cooler, "heightMm", intAttr(cooler, "coolerHeightMm", 0));
        int maxCoolerHeight = intAttr(pcCase, "maxCpuCoolerHeightMm", 190);

        return List.of(
                tool("compatibility",
                        socketMatched && memoryMatched && coolerMatched ? "PASS" : "FAIL",
                        socketMatched && memoryMatched ? "HIGH" : "MEDIUM",
                        socketMatched && memoryMatched && coolerMatched ? "CPU, 메인보드, RAM, 쿨러 기본 호환성이 맞습니다." : "소켓 또는 메모리 호환성 확인이 필요합니다.",
                        MockData.map("socketMatched", socketMatched, "memoryTypeMatched", memoryMatched, "coolerSocketMatched", coolerMatched)),
                tool("power",
                        psuCapacity >= requiredPower ? "PASS" : psuCapacity >= estimatedWattage ? "WARN" : "FAIL",
                        headroom >= 180 ? "HIGH" : "MEDIUM",
                        psuCapacity >= requiredPower ? "PSU 용량이 예상 소비전력과 GPU 권장 전력을 충족합니다." : "PSU 여유 전력이 낮아 상위 용량을 검토해야 합니다.",
                        MockData.map("estimatedWattage", estimatedWattage, "psuCapacityW", psuCapacity, "requiredSystemPowerW", requiredPower, "headroomW", headroom)),
                tool("size",
                        gpuLength <= maxGpuLength && coolerHeight <= maxCoolerHeight ? "PASS" : "WARN",
                        "MEDIUM",
                        gpuLength <= maxGpuLength && coolerHeight <= maxCoolerHeight ? "GPU 길이와 쿨러 높이가 케이스 제약 안에 있습니다." : "케이스 장착 제약을 추가 확인해야 합니다.",
                        MockData.map("gpuLengthMm", gpuLength, "maxGpuLengthMm", maxGpuLength, "coolerHeightMm", coolerHeight, "maxCpuCoolerHeightMm", maxCoolerHeight)),
                tool("performance",
                        intAttr(gpu, "vramGb", 0) >= 12 ? "PASS" : "WARN",
                        "MEDIUM",
                        intAttr(gpu, "vramGb", 0) >= 12 ? "QHD 게임과 개발 병행에 적합한 GPU 등급입니다." : "VRAM 여유가 작아 고해상도 작업에서 한계가 있을 수 있습니다.",
                        MockData.map("gpu", gpu.name(), "vramGb", intAttr(gpu, "vramGb", 0), "cpu", cpu.name())),
                tool("price",
                        total <= budget ? "PASS" : total <= Math.round(budget * 1.08) ? "WARN" : "FAIL",
                        "HIGH",
                        total <= budget ? "저장된 현재가 기준 예산 안에 들어옵니다." : "저장된 현재가 기준 예산을 초과합니다.",
                        MockData.map("budget", budget, "totalPrice", total, "priceDiff", total - budget))
        );
    }

    private Map<String, Object> buildSummary(Map<String, Object> row) {
        String id = DbValueMapper.string(row, "id");
        List<PartCandidate> parts = buildPartCandidates(id);
        Integer totalPrice = DbValueMapper.integer(row, "total_price");
        int budget = budgetForBuild(id, totalPrice == null ? total(parts) : totalPrice);
        List<Map<String, Object>> toolResults = evaluateTools(parts, budget);
        String agentSessionId = agentSessionIdForBuild(id);
        Map<String, Object> agentSession = agentSessionId == null ? Map.of() : agentSession(agentSessionId);
        return MockData.map(
                "id", id,
                "name", DbValueMapper.string(row, "name"),
                "recommendedFor", recommendedFor(DbValueMapper.string(row, "name")),
                "summary", summaryText(DbValueMapper.string(row, "name")),
                "totalPrice", totalPrice,
                "confidence", DbValueMapper.string(row, "confidence"),
                "items", parts.stream().map(this::partItem).toList(),
                "warnings", DbValueMapper.json(row, "warnings", List.of()),
                "evidenceIds", agentSessionId == null ? evidenceIds(id) : evidenceIdsBySession(agentSessionId),
                "agentSessionId", agentSessionId,
                "agentSummary", agentSession.get("summary"),
                "changeableCategories", List.of("CPU", "GPU", "RAM", "STORAGE", "PSU", "CASE", "COOLER"),
                "createdAt", DbValueMapper.timestamp(row, "created_at"),
                "toolResults", toolResults
        );
    }

    private Map<String, Object> partItem(PartCandidate part) {
        return MockData.map(
                "id", part.publicId(),
                "partId", part.publicId(),
                "category", part.category(),
                "name", part.name(),
                "manufacturer", part.manufacturer(),
                "price", part.price(),
                "status", "ACTIVE",
                "attributes", part.attributes()
        );
    }

    private List<PartCandidate> buildPartCandidates(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT p.id AS internal_id,
                               p.public_id::text AS id,
                               p.category,
                               p.name,
                               p.manufacturer,
                               bi.price,
                               p.attributes
                        FROM build_items bi
                        JOIN builds b ON b.id = bi.build_id
                        JOIN parts p ON p.id = bi.part_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY bi.id
                        """, buildId)
                .stream()
                .map(this::partCandidate)
                .toList();
    }

    private List<PartCandidate> partCandidates(String category) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               category,
                               name,
                               manufacturer,
                               price,
                               attributes
                        FROM parts
                        WHERE category = ?
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                          AND coalesce((attributes->>'toolReady')::boolean, false) = true
                        ORDER BY price ASC, id ASC
                        """, category)
                .stream()
                .map(this::partCandidate)
                .toList();
    }

    private PartCandidate partByPublicId(String publicId) {
        return jdbcTemplate.queryForList("""
                        SELECT id AS internal_id,
                               public_id::text AS id,
                               category,
                               name,
                               manufacturer,
                               price,
                               attributes
                        FROM parts
                        WHERE public_id = ?::uuid
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """, publicId)
                .stream()
                .findFirst()
                .map(this::partCandidate)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "변경 대상 부품을 찾을 수 없습니다."));
    }

    private PartCandidate partCandidate(Map<String, Object> row) {
        return new PartCandidate(
                numberLong(row.get("internal_id")),
                DbValueMapper.string(row, "id"),
                DbValueMapper.string(row, "category"),
                DbValueMapper.string(row, "name"),
                DbValueMapper.string(row, "manufacturer"),
                DbValueMapper.integer(row, "price"),
                objectMap(DbValueMapper.json(row, "attributes", Map.of()))
        );
    }

    private Map<String, Object> buildRow(String id) {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, name, total_price, confidence, warnings, created_at
                        FROM builds
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Build를 찾을 수 없습니다."));
    }

    private String runAgent(AgentSessionRoot root) {
        try {
            AgentRunProfile profile = AgentRunProfiles.forRoot(root);
            String sessionId = agentTraceService.createQueuedSession(root, "SYSTEM");
            agentTraceService.advanceStatus(sessionId, AgentStatus.RUNNING, "SYSTEM", "agent run requested for " + profile.purpose());
            agentRunner.run(sessionId, root, profile);
            return sessionId;
        } catch (RuntimeException error) {
            return null;
        }
    }

    private Map<String, Object> agentSession(String id) {
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id, status, summary, state_timeline
                        FROM agent_sessions
                        WHERE public_id = ?::uuid
                        """, id)
                .stream()
                .findFirst()
                .map(row -> MockData.map(
                        "id", DbValueMapper.string(row, "id"),
                        "status", DbValueMapper.string(row, "status"),
                        "summary", DbValueMapper.string(row, "summary"),
                        "stateTimeline", DbValueMapper.json(row, "state_timeline", List.of())
                ))
                .orElse(Map.of());
    }

    private String agentSessionIdForBuild(String buildId) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT s.public_id::text
                FROM agent_sessions s
                JOIN builds b ON b.requirement_id = s.requirement_id OR b.id = s.build_id
                WHERE b.public_id = ?::uuid
                ORDER BY s.created_at DESC, s.id DESC
                LIMIT 1
                """, String.class, buildId);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private List<Map<String, Object>> toolResultsBySession(String sessionId) {
        return jdbcTemplate.queryForList("""
                        SELECT ti.tool_name, ti.status, ti.confidence, ti.summary, ti.result_payload
                        FROM tool_invocations ti
                        JOIN agent_sessions s ON s.id = ti.agent_session_id
                        WHERE s.public_id = ?::uuid
                        ORDER BY ti.id
                        """, sessionId)
                .stream()
                .map(row -> MockData.map(
                        "tool", DbValueMapper.string(row, "tool_name"),
                        "status", DbValueMapper.string(row, "status"),
                        "confidence", DbValueMapper.string(row, "confidence"),
                        "summary", DbValueMapper.string(row, "summary"),
                        "details", DbValueMapper.json(row, "result_payload", Map.of())
                ))
                .toList();
    }

    private List<String> evidenceIdsBySession(String sessionId) {
        return jdbcTemplate.queryForList("""
                        SELECT re.public_id::text
                        FROM rag_evidence re
                        JOIN agent_sessions s ON s.id = re.agent_session_id
                        WHERE s.public_id = ?::uuid
                        ORDER BY re.id
                        """, String.class, sessionId);
    }

    private List<String> evidenceIds(String buildId) {
        return jdbcTemplate.queryForList("""
                        SELECT re.public_id::text
                        FROM rag_evidence re
                        JOIN agent_sessions s ON s.id = re.agent_session_id
                        JOIN builds b ON b.id = s.build_id OR b.requirement_id = s.requirement_id
                        WHERE b.public_id = ?::uuid
                        ORDER BY re.id
                        """, String.class, buildId);
    }

    private int budgetForBuild(String buildId, int fallback) {
        Integer budget = jdbcTemplate.queryForObject("""
                SELECT r.budget
                FROM builds b
                JOIN requirements r ON r.id = b.requirement_id
                WHERE b.public_id = ?::uuid
                """, Integer.class, buildId);
        return budget == null || budget <= 0 ? fallback : budget;
    }

    private String defaultPartId(String category) {
        List<String> rows = jdbcTemplate.queryForList("""
                SELECT public_id::text
                FROM parts
                WHERE category = ?
                  AND status = 'ACTIVE'
                  AND deleted_at IS NULL
                  AND coalesce((attributes->>'toolReady')::boolean, false) = true
                ORDER BY price ASC, id ASC
                LIMIT 1
                """, String.class, category);
        return rows.isEmpty() ? null : rows.get(0);
    }

    private static List<Map<String, Object>> questions(Map<String, Object> parsedContext) {
        List<Map<String, Object>> result = new ArrayList<>();
        if (parsedContext.get("resolution") == null) {
            result.add(question("monitorResolution", "주 모니터 해상도", List.of("FHD", "QHD", "4K")));
        }
        result.add(question("noisePreference", "소음 민감도", List.of("상관없음", "조용한 편", "매우 조용하게")));
        result.add(question("upgradePlan", "업그레이드 여유", List.of("최소", "보통", "넉넉하게")));
        List<?> usageTags = parsedContext.get("usageTags") instanceof List<?> list ? list : List.of();
        if (usageTags.size() > 1) {
            result.add(question("workloadRatio", "게임과 작업 비중", List.of("게임 우선", "작업 우선", "반반")));
        }
        return result;
    }

    private static Map<String, Object> question(String key, String label, List<String> options) {
        return MockData.map("key", key, "label", label, "options", options, "required", false);
    }

    private static List<Map<String, Object>> warningsFor(List<Map<String, Object>> toolResults, int totalPrice, int budget) {
        List<Map<String, Object>> warnings = new ArrayList<>();
        for (Map<String, Object> result : toolResults) {
            String status = String.valueOf(result.get("status"));
            if ("WARN".equals(status) || "FAIL".equals(status)) {
                warnings.add(MockData.map(
                        "code", result.get("tool") + "_" + status,
                        "message", result.get("summary"),
                        "severity", status
                ));
            }
        }
        if (budget > 0 && totalPrice > budget) {
            warnings.add(MockData.map(
                    "code", "OVER_BUDGET",
                    "message", "예산보다 " + (totalPrice - budget) + "원 높습니다.",
                    "severity", totalPrice <= Math.round(budget * 1.08) ? "WARN" : "FAIL"
            ));
        }
        return warnings;
    }

    private static List<Map<String, Object>> combinedWarnings(List<Map<String, Object>> builds) {
        List<Map<String, Object>> result = new ArrayList<>();
        for (Map<String, Object> build : builds) {
            Object warnings = build.get("warnings");
            if (!(warnings instanceof List<?> list)) {
                continue;
            }
            for (Object warning : list) {
                if (warning instanceof Map<?, ?> map) {
                    Map<String, Object> item = new LinkedHashMap<>();
                    map.forEach((key, value) -> item.put(String.valueOf(key), value));
                    result.add(item);
                    if (result.size() >= 8) {
                        return result;
                    }
                }
            }
        }
        return result;
    }

    private List<Map<String, Object>> diffRows(List<PartCandidate> beforeParts, List<PartCandidate> afterParts, String category, int beforeTotal, int afterTotal) {
        PartCandidate before = byCategory(beforeParts).get(category);
        PartCandidate after = byCategory(afterParts).get(category);
        return List.of(
                MockData.map("label", category, "before", before == null ? "-" : before.name(), "after", after == null ? "-" : after.name(), "diff", priceDiff((after == null ? 0 : after.price()) - (before == null ? 0 : before.price())), "status", "PASS"),
                MockData.map("label", "총액", "before", price(beforeTotal), "after", price(afterTotal), "diff", priceDiff(afterTotal - beforeTotal), "status", afterTotal >= beforeTotal ? "WARN" : "PASS"),
                MockData.map("label", "예상 성능", "before", before == null ? "-" : shortSpec(before), "after", after == null ? "-" : shortSpec(after), "diff", "Tool 재검증", "status", "PASS")
        );
    }

    private static List<PartCandidate> replaceCategory(List<PartCandidate> parts, String category, PartCandidate selectedPart) {
        List<PartCandidate> result = new ArrayList<>();
        boolean replaced = false;
        for (PartCandidate part : parts) {
            if (category.equals(part.category())) {
                result.add(selectedPart);
                replaced = true;
            } else {
                result.add(part);
            }
        }
        if (!replaced) {
            result.add(selectedPart);
        }
        return result;
    }

    private static String previousPartId(List<PartCandidate> parts, String category) {
        return parts.stream()
                .filter(part -> category.equals(part.category()))
                .map(PartCandidate::publicId)
                .findFirst()
                .orElse(null);
    }

    private static Map<String, Object> tool(String tool, String status, String confidence, String summary, Map<String, Object> details) {
        return MockData.map("tool", tool, "status", status, "confidence", confidence, "summary", summary, "details", details);
    }

    private static int effectiveBudget(RequirementRow requirement, Map<String, Object> answers) {
        Integer budget = requirement.budget();
        if (budget == null) {
            Object parsedBudget = requirement.parsedContext().get("budget");
            budget = numberValue(parsedBudget);
        }
        if (budget == null || budget <= 0) {
            budget = 2_000_000;
        }
        if ("넉넉하게".equals(answers.get("upgradePlan"))) {
            budget = (int) Math.round(budget * 1.06);
        }
        return budget;
    }

    private static int target(int budget, BuildPlan plan, double allocation) {
        return (int) Math.round(budget * plan.budgetRatio() * allocation);
    }

    private static PartCandidate chooseByTarget(List<PartCandidate> parts, int targetPrice) {
        if (parts.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "추천에 필요한 내부 자산이 부족합니다.");
        }
        return parts.stream()
                .filter(part -> part.price() <= targetPrice)
                .max(Comparator.comparingInt(PartCandidate::price))
                .orElse(parts.get(0));
    }

    private static List<PartCandidate> filter(List<PartCandidate> parts, java.util.function.Predicate<PartCandidate> predicate) {
        return parts.stream().filter(predicate).toList();
    }

    private static List<PartCandidate> orFallback(List<PartCandidate> preferred, List<PartCandidate> fallback) {
        return preferred.isEmpty() ? fallback : preferred;
    }

    private static Map<String, PartCandidate> byCategory(List<PartCandidate> parts) {
        Map<String, PartCandidate> result = new LinkedHashMap<>();
        for (PartCandidate part : parts) {
            result.put(part.category(), part);
        }
        return result;
    }

    private static int total(List<PartCandidate> parts) {
        return parts.stream().mapToInt(part -> part.price() == null ? 0 : part.price()).sum();
    }

    private static int estimatedWattage(List<PartCandidate> parts) {
        return parts.stream()
                .mapToInt(part -> Math.max(intAttr(part, "wattage", 0), intAttr(part, "tdpW", 0)))
                .sum() + 120;
    }

    private static boolean socketSupported(PartCandidate cooler, String socket) {
        if (socket == null) {
            return true;
        }
        Object support = cooler.attributes().get("socketSupport");
        if (support instanceof List<?> list) {
            return list.stream().anyMatch(item -> socket.equalsIgnoreCase(String.valueOf(item)));
        }
        return true;
    }

    private static boolean same(String left, String right) {
        if (left == null || right == null) {
            return true;
        }
        return left.equalsIgnoreCase(right);
    }

    private static String stringAttr(PartCandidate part, String key) {
        if (part == null) {
            return null;
        }
        Object value = part.attributes().get(key);
        return value == null ? null : String.valueOf(value);
    }

    private static int intAttr(PartCandidate part, String key, int fallback) {
        if (part == null) {
            return fallback;
        }
        Object value = part.attributes().get(key);
        Integer parsed = numberValue(value);
        return parsed == null ? fallback : parsed;
    }

    private static String shortSpec(PartCandidate part) {
        return firstText(stringAttr(part, "shortSpec"), part.name());
    }

    private static String recommendedFor(String name) {
        if (name == null) {
            return "맞춤 추천";
        }
        if (name.contains("가성비")) {
            return "예산 우선";
        }
        if (name.contains("고성능")) {
            return "성능 우선";
        }
        return "균형 우선";
    }

    private static String summaryText(String name) {
        return recommendedFor(name) + " 조건으로 내부 자산과 저장된 현재가를 조합했습니다.";
    }

    private static String normalizeCategory(String value) {
        String category = firstText(value, "GPU").toUpperCase(Locale.ROOT);
        if (!BUILD_CATEGORIES.contains(category)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "변경 가능한 부품 카테고리가 아닙니다.");
        }
        return category;
    }

    private static Integer inferBudget(String message) {
        Matcher manwon = BUDGET_MANWON.matcher(message);
        if (manwon.find()) {
            return Integer.parseInt(manwon.group(1)) * 10_000;
        }
        Matcher number = BUDGET_NUMBER.matcher(message);
        if (number.find()) {
            return Integer.parseInt(number.group(1).replace(",", ""));
        }
        return null;
    }

    private static List<String> usageTags(Object value, String message) {
        List<String> provided = stringList(value);
        if (!provided.isEmpty()) {
            return provided;
        }
        String lower = message.toLowerCase(Locale.ROOT);
        Set<String> tags = new LinkedHashSet<>();
        if (lower.contains("게임") || lower.contains("배그") || lower.contains("qhd") || lower.contains("gaming")) {
            tags.add("GAMING");
        }
        if (lower.contains("개발") || lower.contains("ide") || lower.contains("dev")) {
            tags.add("DEVELOPMENT");
        }
        if (lower.contains("영상") || lower.contains("편집")) {
            tags.add("VIDEO_EDIT");
        }
        if (lower.contains("ai")) {
            tags.add("AI_DEV");
        }
        if (tags.isEmpty()) {
            tags.add("GENERAL");
        }
        return new ArrayList<>(tags);
    }

    private static String inferResolution(String message) {
        String upper = message.toUpperCase(Locale.ROOT);
        if (upper.contains("4K") || upper.contains("UHD")) {
            return "4K";
        }
        if (upper.contains("QHD")) {
            return "QHD";
        }
        if (upper.contains("FHD")) {
            return "FHD";
        }
        return null;
    }

    private static List<String> preferredVendors(Object value, String message) {
        List<String> provided = stringList(value);
        if (!provided.isEmpty()) {
            return provided;
        }
        String upper = message.toUpperCase(Locale.ROOT);
        List<String> vendors = new ArrayList<>();
        if (upper.contains("NVIDIA") || upper.contains("RTX")) {
            vendors.add("NVIDIA");
        }
        if (upper.contains("AMD") || upper.contains("RADEON") || upper.contains("RYZEN")) {
            vendors.add("AMD");
        }
        if (upper.contains("INTEL")) {
            vendors.add("INTEL");
        }
        return vendors;
    }

    private static List<String> mustHave(String message) {
        String lower = message.toLowerCase(Locale.ROOT);
        List<String> result = new ArrayList<>();
        if (lower.contains("wifi") || lower.contains("wi-fi") || lower.contains("와이파이")) {
            result.add("WIFI");
        }
        if (lower.contains("저소음") || lower.contains("조용")) {
            result.add("LOW_NOISE");
        }
        return result;
    }

    private static List<String> stringList(Object value) {
        if (value instanceof List<?> list) {
            return list.stream().map(String::valueOf).map(String::trim).filter(item -> !item.isBlank()).toList();
        }
        String text = text(value);
        if (text == null) {
            return List.of();
        }
        return List.of(text.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private static List<String> csv(String value) {
        if (value == null || value.isBlank()) {
            return List.of();
        }
        return List.of(value.split(",")).stream().map(String::trim).filter(item -> !item.isBlank()).toList();
    }

    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        return new LinkedHashMap<>();
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isBlank() || "null".equalsIgnoreCase(text) ? null : text;
    }

    private static String firstText(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private static Integer numberValue(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        String text = text(value);
        if (text == null) {
            return null;
        }
        return Integer.valueOf(text.replace(",", ""));
    }

    private static Long numberLong(Object value) {
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.valueOf(String.valueOf(value));
    }

    private static Map<String, Object> with(Map<String, Object> source, String key, Object value) {
        Map<String, Object> result = new LinkedHashMap<>(source);
        result.put(key, value);
        return result;
    }

    private static String price(int value) {
        return String.format("%,d원", value);
    }

    private static String priceDiff(int value) {
        return (value > 0 ? "+" : "") + price(value);
    }

    private static String json(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value);
        } catch (Exception e) {
            throw new IllegalArgumentException("JSON 변환에 실패했습니다.", e);
        }
    }

    private record RequirementRow(
            Long internalId,
            String publicId,
            String rawMessage,
            Integer budget,
            List<String> usageTags,
            Map<String, Object> parsedContext
    ) {
    }

    private record PartCandidate(
            Long internalId,
            String publicId,
            String category,
            String name,
            String manufacturer,
            Integer price,
            Map<String, Object> attributes
    ) {
    }

    private record BuildPlan(String name, String recommendedFor, int tier, double budgetRatio, String confidence) {
    }
}
