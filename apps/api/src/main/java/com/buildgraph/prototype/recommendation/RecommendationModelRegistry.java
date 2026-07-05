package com.buildgraph.prototype.recommendation;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationModelRegistry {
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final JdbcTemplate jdbcTemplate;

    public RecommendationModelRegistry(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Long upsertShadowModelVersion(Map<String, Object> scorerResponse) throws Exception {
        String modelVersion = firstText(text(scorerResponse.get("modelVersion")), "xgb-shadow-unknown");
        return jdbcTemplate.queryForObject("""
                INSERT INTO recommendation_model_versions (
                  model_name,
                  model_version,
                  algorithm,
                  artifact_path,
                  status,
                  metrics,
                  feature_schema
                )
                VALUES (?, ?, 'XGBOOST', ?, 'SHADOW', ?::jsonb, ?::jsonb)
                ON CONFLICT (model_version)
                DO UPDATE SET status = CASE
                    WHEN recommendation_model_versions.status = 'ACTIVE' THEN recommendation_model_versions.status
                    ELSE 'SHADOW'
                  END
                RETURNING id
                """,
                Long.class,
                firstText(text(scorerResponse.get("modelName")), "xgboost-reranker"),
                modelVersion,
                text(scorerResponse.get("artifactPath")),
                OBJECT_MAPPER.writeValueAsString(objectMap(scorerResponse.get("metrics"))),
                OBJECT_MAPPER.writeValueAsString(objectMap(scorerResponse.get("featureSchema")))
        );
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
            java.util.LinkedHashMap<String, Object> result = new java.util.LinkedHashMap<>();
            map.forEach((key, mapValue) -> result.put(String.valueOf(key), mapValue));
            return result;
        }
        return Map.of();
    }
}
