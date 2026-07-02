package com.buildgraph.prototype.agent;

import com.buildgraph.prototype.common.ApiException;
import com.buildgraph.prototype.common.MockData;
import com.buildgraph.prototype.config.security.AgentPrincipal;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PcAgentLogAnalyzer {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final TypeReference<Map<String, Object>> MAP_TYPE = new TypeReference<>() {
    };
    private static final int RAW_SAMPLE_LIMIT = 20;
    private static final int TIMELINE_LIMIT = 50;
    private static final int SAMPLE_LIST_LIMIT = 5;
    private static final Set<String> REMOTE_SYMPTOMS = Set.of(
            "REMOTE_AGENT",
            "REMOTE_DRIVER_OS",
            "REMOTE_APP_LAUNCHER",
            "REMOTE_STORAGE_MEMORY",
            "REMOTE_STARTUP_SERVICE",
            "REMOTE_LOCAL_NETWORK"
    );
    private static final Set<String> VISIT_SYMPTOMS = Set.of(
            "VISIT_BOOT_REMOTE_BLOCKED",
            "VISIT_DISK_FAILURE",
            "VISIT_WHEA_BSOD",
            "VISIT_POWER_SHUTDOWN",
            "VISIT_FAN_THERMAL"
    );
    private static final Set<String> UNSUPPORTED_SYMPTOMS = Set.of(
            "GAME_FPS_TUNING",
            "OVERCLOCK_STABILITY",
            "ISP_ROUTER",
            "PERIPHERAL_PRINTER",
            "DATA_RECOVERY",
            "ILLEGAL_SOFTWARE",
            "PHYSICAL_DAMAGE"
    );
    private static final Pattern WINDOWS_PATH = Pattern.compile("(?i)[a-z]:\\\\[^\\s\"']+");
    private static final Pattern UNIX_USER_PATH = Pattern.compile("(?i)/(users|home)/[^\\s\"']+");
    private static final Pattern EMAIL = Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}");
    private static final Pattern SECRET = Pattern.compile("(?i)(password|passwd|token|secret|api[_-]?key)[:=][^\\s,;]+");

    private PcAgentLogAnalyzer() {
    }

    static IncidentWindow resolveIncidentWindow(Map<String, Object> metadata, Clock clock) {
        String symptomType = normalizeSymptomType(string(metadata, "symptomType", null), string(metadata, "symptom", ""));
        Instant detectedAt = instant(metadata, "detectedAt", null);
        if (detectedAt == null) {
            detectedAt = instant(metadata, "incidentDetectedAt", Instant.now(clock));
        }
        String triggerType = string(metadata, "triggerType", "USER_REQUEST");
        String incidentId = string(metadata, "incidentId", "incident-" + UUID.randomUUID());
        Integer explicitRangeMinutes = integerOrNull(metadata, "rangeMinutes");
        WindowPolicy policy = policyFor(symptomType, explicitRangeMinutes);

        Instant startedAt = firstInstant(metadata, "incidentStartedAt", "startedAt", "rangeStartedAt");
        Instant endedAt = firstInstant(metadata, "incidentEndedAt", "endedAt", "rangeEndedAt");
        boolean selectedByUser = booleanValue(metadata, "selectedByUser", startedAt != null || endedAt != null);
        if (startedAt == null || endedAt == null) {
            if ("VISIT_BOOT_REMOTE_BLOCKED".equals(symptomType)) {
                startedAt = startedAt == null
                        ? firstInstant(metadata, "lastNormalBootAt", "lastSuccessfulBootAt")
                        : startedAt;
                if (startedAt == null) {
                    startedAt = detectedAt.minus(Duration.ofHours(24));
                }
                endedAt = endedAt == null ? detectedAt : endedAt;
            } else {
                startedAt = startedAt == null ? detectedAt.minusSeconds(policy.preBufferSec()) : startedAt;
                endedAt = endedAt == null ? detectedAt.plusSeconds(policy.postBufferSec()) : endedAt;
            }
        }
        if (!endedAt.isAfter(startedAt)) {
            throw badRequest("incidentWindow endedAt must be after startedAt.");
        }
        return new IncidentWindow(
                incidentId,
                triggerType,
                symptomType,
                detectedAt,
                startedAt,
                endedAt,
                policy.preBufferSec(),
                policy.postBufferSec(),
                selectedByUser,
                string(metadata, "consentId", null)
        );
    }

    static RawLogBundle validateJsonl(String logText, IncidentWindow window) {
        List<RawLogRecord> allRecords = new ArrayList<>();
        List<RawLogRecord> windowRecords = new ArrayList<>();
        int lineNumber = 0;
        int nonBlankLines = 0;
        Integer schemaVersion = null;
        for (String line : logText.lines().toList()) {
            lineNumber++;
            if (line == null || line.isBlank()) {
                continue;
            }
            nonBlankLines++;
            RawLogRecord record = parseLine(line, lineNumber);
            if (schemaVersion == null) {
                schemaVersion = parseSchemaVersion(record.schemaVersion(), lineNumber);
            }
            allRecords.add(record);
            if (!record.collectedAt().isBefore(window.startedAt()) && !record.collectedAt().isAfter(window.endedAt())) {
                windowRecords.add(record);
            }
        }
        if (nonBlankLines == 0) {
            throw fileValidation("Agent log gzip content must contain at least one JSONL log line.");
        }
        return new RawLogBundle(
                schemaVersion == null ? 1 : schemaVersion,
                allRecords,
                windowRecords,
                allRecords.size() - windowRecords.size()
        );
    }

    static AnalysisResult analyze(AgentPrincipal principal, String symptom, IncidentWindow window, RawLogBundle rawLogs) {
        Map<String, List<RawLogRecord>> signalRecords = collectSignalRecords(symptom, rawLogs.windowRecords());
        List<Map<String, Object>> evidenceRefs = new ArrayList<>();
        List<Map<String, Object>> rawSamples = new ArrayList<>();
        LinkedHashMap<String, RawLogRecord> selectedSamples = new LinkedHashMap<>();
        for (Map.Entry<String, List<RawLogRecord>> entry : signalRecords.entrySet()) {
            List<RawLogRecord> records = entry.getValue();
            if (records.isEmpty()) {
                continue;
            }
            for (RawLogRecord record : records) {
                if (selectedSamples.size() >= RAW_SAMPLE_LIMIT) {
                    break;
                }
                selectedSamples.putIfAbsent(record.refId(), record);
                evidenceRefs.add(MockData.map(
                        "refId", record.refId(),
                        "signalCode", entry.getKey(),
                        "sequence", record.sequence(),
                        "collectedAt", record.collectedAt().toString(),
                        "kind", record.kind()
                ));
                if (selectedSamples.size() >= RAW_SAMPLE_LIMIT) {
                    break;
                }
            }
            if (selectedSamples.size() >= RAW_SAMPLE_LIMIT) {
                break;
            }
        }
        selectedSamples.values().forEach(record -> rawSamples.add(record.toRawSample()));

        List<Map<String, Object>> ruleSignals = signalRecords.entrySet()
                .stream()
                .filter(entry -> !entry.getValue().isEmpty())
                .map(entry -> MockData.map(
                        "code", entry.getKey(),
                        "count", entry.getValue().size(),
                        "severity", severity(entry.getKey()),
                        "evidenceRefs", entry.getValue().stream().map(RawLogRecord::refId).limit(RAW_SAMPLE_LIMIT).toList()
                ))
                .toList();
        List<String> reasonCodes = ruleSignals.stream()
                .map(signal -> String.valueOf(signal.get("code")))
                .toList();
        RoutingDraft routing = route(window.symptomType(), symptom, reasonCodes);
        String summaryText = summaryText(window.symptomType(), routing.recommendedDecision(), reasonCodes, rawLogs);
        Map<String, Object> logSummary = MockData.map(
                "summaryVersion", "1",
                "ticketId", null,
                "incidentWindow", window.toMap(),
                "deviceProfile", MockData.map(
                        "deviceId", principal.deviceId(),
                        "agentId", firstAgentId(rawLogs.windowRecords())
                ),
                "userSymptom", MockData.map(
                        "symptomType", window.symptomType(),
                        "description", symptom
                ),
                "baseline", MockData.map(
                        "totalLogLines", rawLogs.allRecords().size(),
                        "windowLogLines", rawLogs.windowRecords().size()
                ),
                "timeline", timeline(rawLogs.windowRecords(), signalRecords),
                "anomalies", anomalies(ruleSignals),
                "correlations", correlations(ruleSignals, window.symptomType()),
                "ruleSignals", ruleSignals,
                "dataQuality", dataQuality(rawLogs, ruleSignals),
                "evidenceRefs", evidenceRefs,
                "rawSamples", rawSamples,
                "summaryText", summaryText
        );
        return new AnalysisResult(rawLogs.schemaVersion(), summaryText, logSummary, routing.toMap());
    }

    static Map<String, Object> withTicketId(Map<String, Object> logSummary, String ticketId) {
        Map<String, Object> result = new LinkedHashMap<>(logSummary);
        result.put("ticketId", ticketId);
        return result;
    }

    static Map<String, Object> aiDiagnosisRequest(String ticketId, Map<String, Object> userSymptom, Map<String, Object> logSummary, Map<String, Object> supportRouting) {
        Object rawSamples = logSummary.getOrDefault("rawSamples", List.of());
        return MockData.map(
                "requestId", "ai-" + UUID.randomUUID(),
                "ticketId", ticketId,
                "userSymptom", userSymptom,
                "logSummary", logSummary,
                "rawSamples", rawSamples,
                "supportRouting", supportRouting,
                "locale", "ko-KR",
                "outputContractVersion", "1"
        );
    }

    private static RawLogRecord parseLine(String line, int lineNumber) {
        JsonNode node;
        try {
            node = OBJECT_MAPPER.readTree(line);
        } catch (JsonProcessingException exception) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " cannot be parsed. Policy: one invalid line fails the upload.");
        }
        requireField(node, "schemaVersion", lineNumber);
        requireField(node, "collectedAt", lineNumber);
        requireField(node, "agentId", lineNumber);
        requireField(node, "sequence", lineNumber);
        requireField(node, "kind", lineNumber);
        JsonNode payloadNode = requireField(node, "payload", lineNumber);
        JsonNode privacyNode = requireField(node, "privacyFlags", lineNumber);
        if (!payloadNode.isObject()) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " payload must be an object.");
        }
        if (!privacyNode.isObject()) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " privacyFlags must be an object.");
        }
        boolean containsRawPath = booleanNode(privacyNode.get("containsRawPath"));
        boolean masked = booleanNode(privacyNode.get("masked"));
        if (containsRawPath && !masked) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " contains raw paths without masking.");
        }
        String collectedAtText = text(node.get("collectedAt"));
        Instant collectedAt;
        try {
            collectedAt = Instant.parse(collectedAtText);
        } catch (RuntimeException exception) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " collectedAt must be ISO-8601.");
        }
        long sequence = node.get("sequence").isNumber()
                ? node.get("sequence").asLong()
                : Long.parseLong(text(node.get("sequence")));
        Map<String, Object> payload = OBJECT_MAPPER.convertValue(payloadNode, MAP_TYPE);
        Map<String, Object> maskedPayload = maskMap(payload);
        return new RawLogRecord(
                "raw-" + lineNumber + "-" + sequence,
                text(node.get("schemaVersion")),
                collectedAt,
                text(node.get("agentId")),
                sequence,
                text(node.get("kind")),
                maskedPayload,
                MockData.map(
                        "masked", true,
                        "containsRawPath", containsRawPath
                )
        );
    }

    private static Map<String, List<RawLogRecord>> collectSignalRecords(String symptom, List<RawLogRecord> records) {
        LinkedHashMap<String, List<RawLogRecord>> result = new LinkedHashMap<>();
        for (String signal : List.of(
                "DRIVER_ERROR_REPEAT",
                "SMART_CRITICAL",
                "DISK_IO_ERROR_REPEAT",
                "WHEA_REPEAT",
                "KERNEL_POWER_REPEAT",
                "THERMAL_SHUTDOWN",
                "FAN_RPM_ZERO",
                "DNS_FAILURE",
                "APP_CRASH_REPEAT",
                "AGENT_UPLOAD_FAILURE",
                "EXTERNAL_NETWORK"
        )) {
            result.put(signal, new ArrayList<>());
        }
        for (RawLogRecord record : records) {
            String text = (record.kind() + " " + record.payload()).toLowerCase(Locale.ROOT);
            if (containsAny(text, "driver", "display driver", "nvlddmkm", "device manager", "windows update failure")) {
                result.get("DRIVER_ERROR_REPEAT").add(record);
            }
            if (containsAny(text, "smart critical", "predictive failure", "bad block")) {
                result.get("SMART_CRITICAL").add(record);
            }
            if (containsAny(text, "disk i/o", "i/o error", "io error", "filesystem error")) {
                result.get("DISK_IO_ERROR_REPEAT").add(record);
            }
            if (containsAny(text, "whea", "bugcheck", "bsod", "blue screen", "hardware error", "memory hardware")) {
                result.get("WHEA_REPEAT").add(record);
            }
            if (containsAny(text, "kernel-power", "kernel power", "event id 41", "unexpected shutdown", "power loss")) {
                result.get("KERNEL_POWER_REPEAT").add(record);
            }
            if (containsAny(text, "thermal shutdown", "thermal throttle", "overheat", "overheated")) {
                result.get("THERMAL_SHUTDOWN").add(record);
            }
            if (containsAny(text, "fan rpm 0", "fan_rpm=0", "fanrpm=0") || numericPayloadEquals(record.payload(), Set.of("fanRpm", "fan_rpm"), 0)) {
                result.get("FAN_RPM_ZERO").add(record);
            }
            if (containsAny(text, "dns failure", "dns timeout", "gateway unreachable", "adapter disabled")) {
                result.get("DNS_FAILURE").add(record);
            }
            if (containsAny(text, "app crash", "application crash", "runtime missing", "launcher crash", "installer error")) {
                result.get("APP_CRASH_REPEAT").add(record);
            }
            if (containsAny(text, "upload failed", "auth 401", "auth 409", "config parse", "agent service stopped", "heartbeat missing")) {
                result.get("AGENT_UPLOAD_FAILURE").add(record);
            }
            if (containsAny(text, "isp", "router", "shared router", "external network", "external service")) {
                result.get("EXTERNAL_NETWORK").add(record);
            }
        }
        result.replaceAll((signal, values) -> shouldKeepRepeatSignal(signal, values) ? values : List.of());
        if (records.isEmpty() && containsAny(symptom.toLowerCase(Locale.ROOT), "부팅", "원격 연결 불가", "boot", "cannot remote")) {
            result.put("AGENT_UPLOAD_FAILURE", List.of());
        }
        return result;
    }

    private static boolean shouldKeepRepeatSignal(String signal, List<RawLogRecord> values) {
        if (values.isEmpty()) {
            return false;
        }
        if (Set.of("SMART_CRITICAL", "THERMAL_SHUTDOWN", "FAN_RPM_ZERO", "DNS_FAILURE", "AGENT_UPLOAD_FAILURE", "EXTERNAL_NETWORK").contains(signal)) {
            return true;
        }
        return values.size() >= 2;
    }

    private static RoutingDraft route(String symptomType, String symptom, List<String> reasonCodes) {
        LinkedHashSet<String> reasons = new LinkedHashSet<>(reasonCodes);
        String decision;
        String confidence = "MEDIUM";
        List<String> remoteActions = new ArrayList<>();
        List<String> visitReasons = new ArrayList<>();
        List<String> blockingFactors = new ArrayList<>();

        if (UNSUPPORTED_SYMPTOMS.contains(symptomType) || unsupportedText(symptom)) {
            decision = "UNSUPPORTED";
            confidence = "HIGH";
            reasons.add("UNSUPPORTED_CATEGORY");
            blockingFactors.add("REMOTE_AND_VISIT_BOOKING_BLOCKED_BY_DEFAULT");
        } else if ("REMOTE_STORAGE_MEMORY".equals(symptomType)
                && (reasonCodes.contains("SMART_CRITICAL") || reasonCodes.contains("DISK_IO_ERROR_REPEAT"))) {
            decision = "REPAIR_OR_REPLACE";
            confidence = "HIGH";
            visitReasons.add("Disk failure signals are unsafe for remote-only handling.");
        } else if ("REMOTE_LOCAL_NETWORK".equals(symptomType) && reasonCodes.contains("EXTERNAL_NETWORK")) {
            decision = "UNSUPPORTED";
            confidence = "HIGH";
            blockingFactors.add("External ISP/router/service issue is outside PC Agent AS scope.");
        } else if (REMOTE_SYMPTOMS.contains(symptomType)) {
            decision = "REMOTE_POSSIBLE";
            confidence = reasonCodes.isEmpty() ? "MEDIUM" : "HIGH";
            remoteActions.addAll(remoteActionsFor(symptomType, reasonCodes));
        } else if ("VISIT_DISK_FAILURE".equals(symptomType)) {
            decision = "REPAIR_OR_REPLACE";
            confidence = "HIGH";
            visitReasons.add("Disk failure symptom requires repair or replacement review.");
        } else if (VISIT_SYMPTOMS.contains(symptomType)) {
            decision = "VISIT_REQUIRED";
            confidence = "HIGH";
            visitReasons.add("Symptom category is visit-required by support policy.");
        } else if (reasonCodes.contains("SMART_CRITICAL") || reasonCodes.contains("DISK_IO_ERROR_REPEAT")) {
            decision = "REPAIR_OR_REPLACE";
            confidence = "HIGH";
            visitReasons.add("Disk failure rule signal matched.");
        } else if (reasonCodes.contains("WHEA_REPEAT") || reasonCodes.contains("KERNEL_POWER_REPEAT")
                || reasonCodes.contains("THERMAL_SHUTDOWN") || reasonCodes.contains("FAN_RPM_ZERO")) {
            decision = "VISIT_REQUIRED";
            confidence = "HIGH";
            visitReasons.add("Hardware-risk rule signal matched.");
        } else if (!reasonCodes.isEmpty()) {
            decision = "REMOTE_POSSIBLE";
            remoteActions.add("ADMIN_REVIEW_RULE_SIGNAL");
        } else {
            decision = "NEEDS_MORE_INFO";
            confidence = "LOW";
            reasons.add("INSUFFICIENT_RULE_SIGNAL");
            blockingFactors.add("No high-signal evidence inside incidentWindow.");
        }
        return new RoutingDraft(decision, confidence, new ArrayList<>(reasons), remoteActions, visitReasons, blockingFactors, true);
    }

    private static List<String> remoteActionsFor(String symptomType, List<String> reasonCodes) {
        return switch (symptomType) {
            case "REMOTE_AGENT" -> List.of("AGENT_RESTART", "AGENT_REREGISTER", "TOKEN_REISSUE_CHECK");
            case "REMOTE_DRIVER_OS" -> List.of("DRIVER_ROLLBACK", "WINDOWS_UPDATE_CHECK");
            case "REMOTE_APP_LAUNCHER" -> List.of("APP_REPAIR", "RUNTIME_INSTALL_CHECK");
            case "REMOTE_STORAGE_MEMORY" -> List.of("TEMP_FILE_CLEANUP", "STARTUP_APP_REVIEW", "PAGEFILE_CHECK");
            case "REMOTE_STARTUP_SERVICE" -> List.of("STARTUP_ITEM_REVIEW", "SERVICE_RESTART_REVIEW");
            case "REMOTE_LOCAL_NETWORK" -> reasonCodes.contains("DNS_FAILURE")
                    ? List.of("DNS_RESET", "ADAPTER_RESET", "NIC_DRIVER_CHECK")
                    : List.of("ADAPTER_RESET", "NIC_DRIVER_CHECK");
            default -> List.of("ADMIN_REVIEW_RULE_SIGNAL");
        };
    }

    private static List<Map<String, Object>> timeline(List<RawLogRecord> records, Map<String, List<RawLogRecord>> signalRecords) {
        Map<String, List<String>> signalByRef = new LinkedHashMap<>();
        signalRecords.forEach((signal, values) -> values.forEach(record ->
                signalByRef.computeIfAbsent(record.refId(), ignored -> new ArrayList<>()).add(signal)));
        return records.stream()
                .sorted(Comparator.comparing(RawLogRecord::collectedAt).thenComparing(RawLogRecord::sequence))
                .limit(TIMELINE_LIMIT)
                .map(record -> MockData.map(
                        "collectedAt", record.collectedAt().toString(),
                        "kind", record.kind(),
                        "sequence", record.sequence(),
                        "signals", signalByRef.getOrDefault(record.refId(), List.of())
                ))
                .toList();
    }

    private static List<Map<String, Object>> anomalies(List<Map<String, Object>> ruleSignals) {
        return ruleSignals.stream()
                .map(signal -> MockData.map(
                        "code", signal.get("code"),
                        "severity", signal.get("severity"),
                        "count", signal.get("count")
                ))
                .toList();
    }

    private static List<Map<String, Object>> correlations(List<Map<String, Object>> ruleSignals, String symptomType) {
        if (ruleSignals.isEmpty()) {
            return List.of();
        }
        return List.of(MockData.map(
                "type", "SYMPTOM_RULE_SIGNAL",
                "symptomType", symptomType,
                "signalCodes", ruleSignals.stream().map(signal -> signal.get("code")).toList()
        ));
    }

    private static Map<String, Object> dataQuality(RawLogBundle rawLogs, List<Map<String, Object>> ruleSignals) {
        List<String> missingSignals = new ArrayList<>();
        if (rawLogs.windowRecords().isEmpty()) {
            missingSignals.add("NO_LOGS_IN_INCIDENT_WINDOW");
        }
        if (ruleSignals.isEmpty()) {
            missingSignals.add("NO_HIGH_SIGNAL_RULE_MATCH");
        }
        String level = missingSignals.isEmpty() ? "ENOUGH" : rawLogs.windowRecords().isEmpty() ? "INSUFFICIENT" : "PARTIAL";
        return MockData.map(
                "level", level,
                "missingSignals", missingSignals,
                "parseFailurePolicy", "FAIL_WHOLE_UPLOAD_ON_BAD_JSONL_LINE",
                "totalLines", rawLogs.allRecords().size(),
                "usedLines", rawLogs.windowRecords().size(),
                "filteredOutOfWindow", rawLogs.filteredOutOfWindow(),
                "rawSampleLimit", RAW_SAMPLE_LIMIT
        );
    }

    private static String summaryText(String symptomType, String decision, List<String> reasonCodes, RawLogBundle rawLogs) {
        String signals = reasonCodes.isEmpty() ? "no high-signal rule" : String.join(",", reasonCodes);
        return "IncidentWindow summary: " + rawLogs.windowRecords().size() + "/"
                + rawLogs.allRecords().size() + " logs used, symptomType=" + symptomType
                + ", decision=" + decision + ", signals=" + signals + ".";
    }

    private static String firstAgentId(List<RawLogRecord> records) {
        return records.stream().findFirst().map(RawLogRecord::agentId).orElse(null);
    }

    private static String severity(String signal) {
        if (Set.of("SMART_CRITICAL", "DISK_IO_ERROR_REPEAT", "WHEA_REPEAT", "KERNEL_POWER_REPEAT", "THERMAL_SHUTDOWN", "FAN_RPM_ZERO").contains(signal)) {
            return "HIGH";
        }
        if ("EXTERNAL_NETWORK".equals(signal)) {
            return "MEDIUM";
        }
        return "MEDIUM";
    }

    private static String normalizeSymptomType(String explicit, String symptom) {
        if (explicit != null && !explicit.isBlank()) {
            return explicit.trim().toUpperCase(Locale.ROOT);
        }
        String text = symptom == null ? "" : symptom.toLowerCase(Locale.ROOT);
        if (containsAny(text, "boot", "부팅", "원격 연결 불가")) {
            return "VISIT_BOOT_REMOTE_BLOCKED";
        }
        if (containsAny(text, "smart", "disk", "디스크", "ssd")) {
            return "VISIT_DISK_FAILURE";
        }
        if (unsupportedText(symptom)) {
            return "GAME_FPS_TUNING";
        }
        return "REMOTE_AGENT";
    }

    private static WindowPolicy policyFor(String symptomType, Integer explicitRangeMinutes) {
        if ("VISIT_BOOT_REMOTE_BLOCKED".equals(symptomType)) {
            return new WindowPolicy(0, 0);
        }
        if (VISIT_SYMPTOMS.contains(symptomType) || Set.of("VISIT_DISK_FAILURE", "VISIT_WHEA_BSOD", "VISIT_POWER_SHUTDOWN", "VISIT_FAN_THERMAL").contains(symptomType)) {
            return new WindowPolicy(1800, 600);
        }
        if (REMOTE_SYMPTOMS.contains(symptomType)) {
            return new WindowPolicy(900, 300);
        }
        if (explicitRangeMinutes != null && explicitRangeMinutes > 0) {
            return new WindowPolicy(explicitRangeMinutes * 60, 0);
        }
        return new WindowPolicy(900, 300);
    }

    private static boolean unsupportedText(String symptom) {
        String text = symptom == null ? "" : symptom.toLowerCase(Locale.ROOT);
        return containsAny(text, "fps tuning", "게임별 fps", "overclock", "오버클럭", "isp", "router", "공유기", "printer", "프린터", "data recovery", "데이터 복구", "illegal", "불법", "physical damage", "물리 파손");
    }

    private static JsonNode requireField(JsonNode node, String fieldName, int lineNumber) {
        JsonNode value = node.get(fieldName);
        if (value == null || value.isNull()) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " missing required field: " + fieldName + ".");
        }
        return value;
    }

    private static int parseSchemaVersion(String schemaVersion, int lineNumber) {
        try {
            return Integer.parseInt(schemaVersion);
        } catch (NumberFormatException exception) {
            throw fileValidation("Agent log JSONL line " + lineNumber + " schemaVersion must be numeric.");
        }
    }

    private static boolean numericPayloadEquals(Map<String, Object> payload, Set<String> keys, int expected) {
        for (String key : keys) {
            Object value = payload.get(key);
            if (value instanceof Number number && number.intValue() == expected) {
                return true;
            }
            if (value != null && String.valueOf(expected).equals(value.toString())) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Object> maskMap(Map<String, Object> payload) {
        Map<String, Object> masked = new LinkedHashMap<>();
        payload.forEach((key, value) -> masked.put(key, maskValue(value)));
        return masked;
    }

    private static Map<String, Object> samplePayload(Map<String, Object> payload) {
        Map<String, Object> sample = new LinkedHashMap<>();
        payload.forEach((key, value) -> sample.put(key, sampleValue(value)));
        return sample;
    }

    private static Object maskValue(Object value) {
        if (value instanceof String text) {
            return maskText(text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> masked = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> masked.put(String.valueOf(key), maskValue(nestedValue)));
            return masked;
        }
        if (value instanceof List<?> list) {
            return list.stream().map(PcAgentLogAnalyzer::maskValue).toList();
        }
        return value;
    }

    private static Object sampleValue(Object value) {
        if (value instanceof String text) {
            return maskText(text);
        }
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> sample = new LinkedHashMap<>();
            map.forEach((key, nestedValue) -> sample.put(String.valueOf(key), sampleValue(nestedValue)));
            return sample;
        }
        if (value instanceof List<?> list) {
            List<?> sampleItems = list.stream()
                    .limit(SAMPLE_LIST_LIMIT)
                    .map(PcAgentLogAnalyzer::sampleValue)
                    .toList();
            if (list.size() <= SAMPLE_LIST_LIMIT) {
                return sampleItems;
            }
            return MockData.map(
                    "sample", sampleItems,
                    "originalCount", list.size(),
                    "truncated", true
            );
        }
        return value;
    }

    private static String maskText(String text) {
        String masked = WINDOWS_PATH.matcher(text).replaceAll("[PATH]");
        masked = UNIX_USER_PATH.matcher(masked).replaceAll("[PATH]");
        masked = EMAIL.matcher(masked).replaceAll("[EMAIL]");
        masked = SECRET.matcher(masked).replaceAll("$1=[REDACTED]");
        return masked;
    }

    private static Instant firstInstant(Map<String, Object> metadata, String... keys) {
        for (String key : keys) {
            Instant value = instant(metadata, key, null);
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    private static String string(Map<String, Object> request, String key, String fallback) {
        if (request == null || request.get(key) == null) {
            return fallback;
        }
        String value = request.get(key).toString();
        return value.isBlank() ? fallback : value;
    }

    private static Integer integerOrNull(Map<String, Object> request, String key) {
        if (request == null || request.get(key) == null) {
            return null;
        }
        Object value = request.get(key);
        return value instanceof Number number ? number.intValue() : Integer.parseInt(value.toString());
    }

    private static Instant instant(Map<String, Object> request, String key, Instant fallback) {
        if (request == null || request.get(key) == null) {
            return fallback;
        }
        return Instant.parse(request.get(key).toString());
    }

    private static boolean booleanValue(Map<String, Object> request, String key, boolean fallback) {
        if (request == null || request.get(key) == null) {
            return fallback;
        }
        Object value = request.get(key);
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        return Boolean.parseBoolean(value.toString());
    }

    private static boolean booleanNode(JsonNode node) {
        if (node == null || node.isNull()) {
            return false;
        }
        return node.isBoolean() ? node.booleanValue() : Boolean.parseBoolean(node.asText());
    }

    private static String text(JsonNode node) {
        return node.isTextual() ? node.asText() : node.toString().replace("\"", "");
    }

    private static boolean containsAny(String text, String... needles) {
        for (String needle : needles) {
            if (text.contains(needle)) {
                return true;
            }
        }
        return false;
    }

    private static ResponseStatusException badRequest(String message) {
        return new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
    }

    private static ApiException fileValidation(String message) {
        return new ApiException(HttpStatus.BAD_REQUEST, "FILE_VALIDATION_ERROR", message);
    }

    record IncidentWindow(
            String incidentId,
            String triggerType,
            String symptomType,
            Instant detectedAt,
            Instant startedAt,
            Instant endedAt,
            int preBufferSec,
            int postBufferSec,
            boolean selectedByUser,
            String consentId
    ) {
        Map<String, Object> toMap() {
            return MockData.map(
                    "incidentId", incidentId,
                    "triggerType", triggerType,
                    "symptomType", symptomType,
                    "detectedAt", detectedAt.toString(),
                    "startedAt", startedAt.toString(),
                    "endedAt", endedAt.toString(),
                    "preBufferSec", preBufferSec,
                    "postBufferSec", postBufferSec,
                    "selectedByUser", selectedByUser,
                    "consentId", consentId
            );
        }

        int durationMinutes() {
            long seconds = Duration.between(startedAt, endedAt).toSeconds();
            return Math.max(1, (int) Math.ceil(seconds / 60.0d));
        }
    }

    record RawLogBundle(
            int schemaVersion,
            List<RawLogRecord> allRecords,
            List<RawLogRecord> windowRecords,
            int filteredOutOfWindow
    ) {
    }

    record RawLogRecord(
            String refId,
            String schemaVersion,
            Instant collectedAt,
            String agentId,
            long sequence,
            String kind,
            Map<String, Object> payload,
            Map<String, Object> privacyFlags
    ) {
        Map<String, Object> toRawSample() {
            return MockData.map(
                    "refId", refId,
                    "schemaVersion", schemaVersion,
                    "collectedAt", collectedAt.toString(),
                    "agentId", agentId,
                    "sequence", sequence,
                    "kind", kind,
                    "payload", samplePayload(payload),
                    "privacyFlags", privacyFlags
            );
        }
    }

    record AnalysisResult(
            int schemaVersion,
            String summaryText,
            Map<String, Object> logSummary,
            Map<String, Object> supportRouting
    ) {
    }

    private record WindowPolicy(int preBufferSec, int postBufferSec) {
    }

    private record RoutingDraft(
            String recommendedDecision,
            String confidence,
            List<String> reasonCodes,
            List<String> remoteActions,
            List<String> visitReasons,
            List<String> blockingFactors,
            boolean requiresAdminApproval
    ) {
        Map<String, Object> toMap() {
            return MockData.map(
                    "recommendedDecision", recommendedDecision,
                    "confidence", confidence,
                    "reasonCodes", reasonCodes,
                    "remoteActions", remoteActions,
                    "visitReasons", visitReasons,
                    "blockingFactors", blockingFactors,
                    "requiresAdminApproval", requiresAdminApproval
            );
        }
    }
}
