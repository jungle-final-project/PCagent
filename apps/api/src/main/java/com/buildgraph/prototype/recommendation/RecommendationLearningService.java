package com.buildgraph.prototype.recommendation;

import com.buildgraph.prototype.common.DbValueMapper;
import com.buildgraph.prototype.common.MockData;
import com.buildgraph.prototype.user.CurrentUserService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationLearningService {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final Map<String, Double> LABEL_SCORES = Map.of(
            "IMPRESSION", 0.0,
            "CLICK", 1.0,
            "DETAIL_VIEW", 1.0,
            "SAVE", 3.0,
            "CHANGE_ADOPTED", 3.0,
            "ADD_BUILD_TO_DRAFT", 3.0,
            "ORDER_INTENT", 5.0,
            "REJECT", -1.0,
            "CHANGE_REVERTED", -1.0,
            "AS_CONFIRMED_NEGATIVE", -2.0
    );

    private final JdbcTemplate jdbcTemplate;

    public RecommendationLearningService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Map<String, Object> recordEvent(Map<String, Object> request, CurrentUserService.CurrentUser user) {
        String eventType = normalizeEventType(text(request.get("eventType")));
        if ("AS_CONFIRMED_NEGATIVE".equals(eventType)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "AS 확정 피드백은 관리자 API를 사용해야 합니다.");
        }
        String idempotencyKey = text(request.get("idempotencyKey"));
        if (idempotencyKey != null) {
            Map<String, Object> existing = findEventByIdempotency(user.internalId(), idempotencyKey);
            if (!existing.isEmpty()) {
                return existing;
            }
        }
        Long partId = resolvePartId(text(request.get("partId")));
        Long buildId = resolveOwnedBuildId(text(request.get("buildId")), user.internalId());
        Long asTicketId = resolveOwnedAsTicketId(text(request.get("asTicketId")), user.internalId());
        return insertEvent(
                user.internalId(),
                null,
                eventType,
                label(eventType),
                firstText(text(request.get("sourceSurface")), "BUILD_CHAT"),
                text(request.get("recommendationId")),
                buildId,
                partId,
                asTicketId,
                text(request.get("category")),
                integer(request.get("rankPosition")),
                idempotencyKey,
                eventPayload(request)
        );
    }

    public Map<String, Object> confirmAsNegativeFeedback(
            String ticketPublicId,
            Map<String, Object> request,
            CurrentUserService.CurrentUser admin
    ) {
        Map<String, Object> ticket = jdbcTemplate.queryForList("""
                        SELECT id,
                               user_id,
                               public_id::text AS public_id,
                               symptom,
                               status
                        FROM as_tickets
                        WHERE public_id = ?::uuid
                          AND deleted_at IS NULL
                        """, ticketPublicId)
                .stream()
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AS 티켓을 찾을 수 없습니다."));
        String idempotencyKey = "AS_CONFIRMED_NEGATIVE:" + ticket.get("id");
        Map<String, Object> existing = findEventByIdempotency(longValue(ticket, "user_id"), idempotencyKey);
        if (!existing.isEmpty()) {
            return existing;
        }
        Long partId = resolvePartId(text(request.get("relatedPartId")));
        Map<String, Object> payload = new LinkedHashMap<>(eventPayload(request));
        payload.put("ticketId", ticketPublicId);
        payload.put("ticketStatus", ticket.get("status"));
        payload.put("ticketSymptom", ticket.get("symptom"));
        payload.put("confirmedByAdminId", admin.id());
        return insertEvent(
                longValue(ticket, "user_id"),
                admin.internalId(),
                "AS_CONFIRMED_NEGATIVE",
                label("AS_CONFIRMED_NEGATIVE"),
                "ADMIN_AS_FEEDBACK",
                text(request.get("recommendationId")),
                null,
                partId,
                longValue(ticket, "id"),
                text(request.get("category")),
                null,
                idempotencyKey,
                payload
        );
    }

    public Map<String, Object> modelVersions() {
        List<Map<String, Object>> items = jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id,
                               model_name,
                               model_version,
                               algorithm,
                               artifact_path,
                               status,
                               trained_from,
                               trained_to,
                               metrics,
                               feature_schema,
                               activated_at,
                               created_at
                        FROM recommendation_model_versions
                        WHERE deleted_at IS NULL
                        ORDER BY created_at DESC, id DESC
                        LIMIT 50
                        """)
                .stream()
                .map(this::modelVersionDto)
                .toList();
        return MockData.map("items", items, "page", 0, "size", 50, "total", items.size());
    }

    private Map<String, Object> insertEvent(
            Long userId,
            Long adminId,
            String eventType,
            double labelScore,
            String sourceSurface,
            String recommendationId,
            Long buildId,
            Long partId,
            Long asTicketId,
            String category,
            Integer rankPosition,
            String idempotencyKey,
            Map<String, Object> payload
    ) {
        try {
            return eventDto(jdbcTemplate.queryForMap("""
                    INSERT INTO recommendation_events (
                      user_id,
                      created_by_admin_id,
                      event_type,
                      label_score,
                      source_surface,
                      recommendation_id,
                      build_id,
                      part_id,
                      as_ticket_id,
                      category,
                      rank_position,
                      idempotency_key,
                      event_payload
                    )
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb)
                    RETURNING public_id::text AS id,
                              event_type,
                              label_score,
                              source_surface,
                              recommendation_id,
                              category,
                              rank_position,
                              created_at
                    """,
                    userId,
                    adminId,
                    eventType,
                    labelScore,
                    sourceSurface,
                    recommendationId,
                    buildId,
                    partId,
                    asTicketId,
                    normalizeCategory(category),
                    rankPosition,
                    idempotencyKey,
                    toJson(payload)
            ));
        } catch (DataAccessException error) {
            if (idempotencyKey != null) {
                Map<String, Object> existing = findEventByIdempotency(userId, idempotencyKey);
                if (!existing.isEmpty()) {
                    return existing;
                }
            }
            throw error;
        }
    }

    private Map<String, Object> findEventByIdempotency(Long userId, String idempotencyKey) {
        if (userId == null || idempotencyKey == null || idempotencyKey.isBlank()) {
            return Map.of();
        }
        return jdbcTemplate.queryForList("""
                        SELECT public_id::text AS id,
                               event_type,
                               label_score,
                               source_surface,
                               recommendation_id,
                               category,
                               rank_position,
                               created_at
                        FROM recommendation_events
                        WHERE user_id = ?
                          AND idempotency_key = ?
                        """, userId, idempotencyKey)
                .stream()
                .findFirst()
                .map(this::eventDto)
                .orElse(Map.of());
    }

    private Long resolvePartId(String partPublicId) {
        if (partPublicId == null) {
            return null;
        }
        return jdbcTemplate.queryForList("""
                        SELECT id
                        FROM parts
                        WHERE public_id = ?::uuid
                          AND status = 'ACTIVE'
                          AND deleted_at IS NULL
                        """, partPublicId)
                .stream()
                .findFirst()
                .map(row -> longValue(row, "id"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "부품을 찾을 수 없습니다."));
    }

    private Long resolveOwnedBuildId(String buildPublicId, Long userId) {
        if (buildPublicId == null) {
            return null;
        }
        return jdbcTemplate.queryForList("""
                        SELECT b.id
                        FROM builds b
                        JOIN requirements r ON r.id = b.requirement_id
                        WHERE b.public_id = ?::uuid
                          AND r.user_id = ?
                        """, buildPublicId, userId)
                .stream()
                .findFirst()
                .map(row -> longValue(row, "id"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "추천 견적을 찾을 수 없습니다."));
    }

    private Long resolveOwnedAsTicketId(String ticketPublicId, Long userId) {
        if (ticketPublicId == null) {
            return null;
        }
        return jdbcTemplate.queryForList("""
                        SELECT id
                        FROM as_tickets
                        WHERE public_id = ?::uuid
                          AND user_id = ?
                          AND deleted_at IS NULL
                        """, ticketPublicId, userId)
                .stream()
                .findFirst()
                .map(row -> longValue(row, "id"))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "AS 티켓을 찾을 수 없습니다."));
    }

    private String normalizeEventType(String eventType) {
        if (eventType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "eventType은 필수입니다.");
        }
        String normalized = eventType.trim().toUpperCase(Locale.ROOT);
        if (!LABEL_SCORES.containsKey(normalized)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "지원하지 않는 recommendation eventType입니다.");
        }
        return normalized;
    }

    private static double label(String eventType) {
        return LABEL_SCORES.getOrDefault(eventType, 0.0);
    }

    private Map<String, Object> eventPayload(Map<String, Object> request) {
        Map<String, Object> payload = new LinkedHashMap<>();
        for (String key : List.of("message", "reason", "intent", "aiProfile", "modelVersion", "metadata", "eventPayload")) {
            Object value = request.get(key);
            if (value != null) {
                payload.put(key, value);
            }
        }
        return payload;
    }

    private Map<String, Object> eventDto(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "eventType", DbValueMapper.string(row, "event_type"),
                "labelScore", row.get("label_score"),
                "sourceSurface", DbValueMapper.string(row, "source_surface"),
                "recommendationId", DbValueMapper.string(row, "recommendation_id"),
                "category", DbValueMapper.string(row, "category"),
                "rankPosition", DbValueMapper.integer(row, "rank_position"),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }

    private Map<String, Object> modelVersionDto(Map<String, Object> row) {
        return MockData.map(
                "id", DbValueMapper.string(row, "id"),
                "modelName", DbValueMapper.string(row, "model_name"),
                "modelVersion", DbValueMapper.string(row, "model_version"),
                "algorithm", DbValueMapper.string(row, "algorithm"),
                "artifactPath", DbValueMapper.string(row, "artifact_path"),
                "status", DbValueMapper.string(row, "status"),
                "trainedFrom", DbValueMapper.timestamp(row, "trained_from"),
                "trainedTo", DbValueMapper.timestamp(row, "trained_to"),
                "metrics", DbValueMapper.json(row, "metrics", Map.of()),
                "featureSchema", DbValueMapper.json(row, "feature_schema", Map.of()),
                "activatedAt", DbValueMapper.timestamp(row, "activated_at"),
                "createdAt", DbValueMapper.timestamp(row, "created_at")
        );
    }

    private String normalizeCategory(String category) {
        if (category == null || category.isBlank()) {
            return null;
        }
        return category.trim().toUpperCase(Locale.ROOT);
    }

    private String toJson(Object value) {
        try {
            return OBJECT_MAPPER.writeValueAsString(value == null ? Map.of() : value);
        } catch (Exception error) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON payload 직렬화에 실패했습니다.", error);
        }
    }

    private static String firstText(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private static String text(Object value) {
        if (value == null) {
            return null;
        }
        String text = value.toString().trim();
        return text.isBlank() ? null : text;
    }

    private static Integer integer(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Integer.valueOf(value.toString());
    }

    private static long longValue(Map<String, Object> row, String key) {
        Object value = row.get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
