package com.buildgraph.prototype.recommendation;

import com.buildgraph.prototype.common.MockData;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class XgboostShadowReranker implements CandidateReranker {
    private static final Logger log = LoggerFactory.getLogger(XgboostShadowReranker.class);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;
    private final RecommendationScoringClient scoringClient;
    private final RecommendationModelRegistry modelRegistry;
    private final boolean activeRerankEnabled;
    private final boolean shadowEnabled;

    public XgboostShadowReranker(
            JdbcTemplate jdbcTemplate,
            RecommendationScoringClient scoringClient,
            RecommendationModelRegistry modelRegistry,
            @Value("${recommendation.reranker.enabled:false}") boolean activeRerankEnabled,
            @Value("${recommendation.reranker.shadow-enabled:true}") boolean shadowEnabled
    ) {
        this.jdbcTemplate = jdbcTemplate;
        this.scoringClient = scoringClient;
        this.modelRegistry = modelRegistry;
        this.activeRerankEnabled = activeRerankEnabled;
        this.shadowEnabled = shadowEnabled;
    }

    @Override
    public void recordShadowScores(Map<String, Object> request, Map<String, Object> response, Long userId, String requestedAiProfile) {
        if (!shadowEnabled) {
            return;
        }
        try {
            List<CandidateFeature> candidates = candidates(response);
            if (candidates.isEmpty()) {
                return;
            }
            String requestHash = sha256(OBJECT_MAPPER.writeValueAsString(MockData.map(
                    "request", request == null ? Map.of() : request,
                    "profile", requestedAiProfile == null ? "" : requestedAiProfile,
                    "candidateIds", candidates.stream().map(CandidateFeature::candidateId).toList()
            )));
            Map<String, Object> payload = scoringClient.payload(
                    requestHash,
                    requestedAiProfile,
                    activeRerankEnabled,
                    candidates.stream().map(CandidateFeature::toPayload).toList()
            );
            Map<String, Object> scorerResponse = scoringClient.score(payload);
            List<Map<String, Object>> scores = objectMaps(scorerResponse.get("scores"));
            if (scores.isEmpty()) {
                return;
            }
            Long modelVersionId = modelRegistry.upsertShadowModelVersion(scorerResponse);
            Map<String, CandidateFeature> byCandidateId = new LinkedHashMap<>();
            Map<String, CandidateFeature> byPartId = new LinkedHashMap<>();
            for (CandidateFeature candidate : candidates) {
                byCandidateId.put(candidate.candidateId(), candidate);
                if (candidate.partId() != null) {
                    byPartId.put(candidate.partId(), candidate);
                }
            }
            for (Map<String, Object> scoreRow : scores) {
                CandidateFeature candidate = firstPresent(
                        byCandidateId.get(text(scoreRow.get("candidateId"))),
                        byPartId.get(text(scoreRow.get("partId")))
                ).orElse(null);
                Double score = decimal(scoreRow.get("score"));
                if (candidate == null || score == null) {
                    continue;
                }
                Long internalPartId = resolvePartInternalId(candidate.partId()).orElse(null);
                jdbcTemplate.update("""
                        INSERT INTO recommendation_shadow_scores (
                          user_id,
                          model_version_id,
                          source_surface,
                          request_hash,
                          candidate_type,
                          candidate_id,
                          part_id,
                          score,
                          rank_position,
                          features,
                          raw_response
                        )
                        VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?::jsonb, ?::jsonb)
                        """,
                        userId,
                        modelVersionId,
                        "BUILD_CHAT",
                        requestHash,
                        candidate.candidateType(),
                        candidate.candidateId(),
                        internalPartId,
                        score,
                        candidate.rankPosition(),
                        OBJECT_MAPPER.writeValueAsString(candidate.features()),
                        OBJECT_MAPPER.writeValueAsString(scoreRow)
                );
            }
        } catch (Exception error) {
            log.warn("XGBoost shadow scoring skipped: {}", error.getMessage());
        }
    }

    private List<CandidateFeature> candidates(Map<String, Object> response) {
        List<CandidateFeature> result = new ArrayList<>();
        List<Map<String, Object>> builds = objectMaps(response.get("builds"));
        for (int index = 0; index < builds.size(); index += 1) {
            Map<String, Object> build = builds.get(index);
            String candidateId = text(build.get("id"));
            if (candidateId == null) {
                continue;
            }
            Map<String, Object> features = new LinkedHashMap<>();
            features.put("totalPrice", integer(build.get("totalPrice")));
            features.put("build_total_price", integer(build.get("totalPrice")));
            features.put("budgetWon", integer(build.get("budgetWon")));
            features.put("itemCount", objectMaps(build.get("items")).size());
            features.put("warningCount", strings(build.get("warnings")).size());
            features.put("toolWarningCount", toolWarningCount(objectMaps(build.get("toolResults"))));
            features.put("evidenceCount", strings(build.get("evidenceIds")).size());
            features.put("confidence", text(build.get("confidence")));
            features.put("tier", text(build.get("tier")));
            result.add(new CandidateFeature("BUILD", candidateId, null, index, features));
        }
        Map<String, Object> partRecommendation = objectMap(response.get("partRecommendation"));
        List<Map<String, Object>> options = objectMaps(partRecommendation.get("options"));
        for (int index = 0; index < options.size(); index += 1) {
            Map<String, Object> option = options.get(index);
            String partId = text(option.get("partId"));
            if (partId == null) {
                continue;
            }
            Map<String, Object> features = new LinkedHashMap<>();
            features.put("category", text(option.get("category")));
            features.put("price", integer(option.get("price")));
            features.put("part_price", integer(option.get("price")));
            features.put("quantity", integer(option.get("quantity")));
            features.put("manufacturer", text(option.get("manufacturer")));
            features.put("hasAttributes", !objectMap(option.get("attributes")).isEmpty());
            result.add(new CandidateFeature("PART", partId, partId, index, features));
        }
        return result.stream()
                .sorted(Comparator.comparing(CandidateFeature::candidateType).thenComparing(CandidateFeature::rankPosition))
                .toList();
    }

    private Optional<Long> resolvePartInternalId(String partPublicId) {
        if (partPublicId == null) {
            return Optional.empty();
        }
        return jdbcTemplate.queryForList("SELECT id FROM parts WHERE public_id = ?::uuid", partPublicId)
                .stream()
                .findFirst()
                .map(row -> ((Number) row.get("id")).longValue());
    }

    private int toolWarningCount(List<Map<String, Object>> toolResults) {
        int count = 0;
        for (Map<String, Object> result : toolResults) {
            String status = text(result.get("status"));
            if ("WARN".equals(status) || "FAIL".equals(status)) {
                count += 1;
            }
        }
        return count;
    }

    private static String sha256(String value) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(value.getBytes(StandardCharsets.UTF_8));
        StringBuilder result = new StringBuilder();
        for (byte b : hash) {
            result.append(String.format("%02x", b));
        }
        return result.toString();
    }

    @SafeVarargs
    private static <T> Optional<T> firstPresent(T... values) {
        for (T value : values) {
            if (value != null) {
                return Optional.of(value);
            }
        }
        return Optional.empty();
    }

    private static Double decimal(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null || value.toString().isBlank()) {
            return null;
        }
        return Double.valueOf(value.toString());
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

    private static Map<String, Object> objectMap(Object value) {
        if (value instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        return Map.of();
    }

    private static List<Map<String, Object>> objectMaps(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        List<Map<String, Object>> result = new ArrayList<>();
        for (Object item : list) {
            result.add(objectMap(item));
        }
        return result;
    }

    private static List<String> strings(Object value) {
        if (!(value instanceof List<?> list)) {
            return List.of();
        }
        return list.stream().filter(Objects::nonNull).map(Object::toString).toList();
    }

    private record CandidateFeature(
            String candidateType,
            String candidateId,
            String partId,
            int rankPosition,
            Map<String, Object> features
    ) {
        Map<String, Object> toPayload() {
            return MockData.map(
                    "candidateType", candidateType,
                    "candidateId", candidateId,
                    "partId", partId,
                    "rankPosition", rankPosition,
                    "features", features
            );
        }
    }
}
