package com.buildgraph.prototype.agent;

import com.buildgraph.prototype.common.MockData;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class GeminiGenerateContentClient {
    private static final ParameterizedTypeReference<Map<String, Object>> MAP_RESPONSE =
            new ParameterizedTypeReference<>() {
            };

    private final RestClient restClient;
    private final String apiKey;

    public GeminiGenerateContentClient(
            @Value("${gemini.base-url:https://generativelanguage.googleapis.com/v1beta}") String baseUrl,
            @Value("${gemini.api-key:}") String apiKey
    ) {
        this.restClient = RestClient.builder()
                .baseUrl(trimTrailingSlash(baseUrl))
                .build();
        this.apiKey = blankToNull(apiKey);
    }

    public boolean isConfigured() {
        return apiKey != null;
    }

    public LlmResponseResult createStructuredJsonResult(
            String systemPrompt,
            String userPrompt,
            String schemaName,
            Map<String, Object> jsonSchema,
            String requestedModel,
            String requestedReasoningEffort,
            Integer requestedMaxOutputTokens
    ) {
        if (!isConfigured()) {
            throw new ResponseStatusException(HttpStatus.PRECONDITION_REQUIRED, "GEMINI_API_KEY가 필요합니다.");
        }
        String effectiveModel = normalizeModel(requestedModel);
        Map<String, Object> request = requestBody(systemPrompt, userPrompt, jsonSchema, requestedMaxOutputTokens);
        long startedAt = System.nanoTime();
        Map<String, Object> response;
        try {
            response = restClient.post()
                    .uri("/models/{model}:generateContent?key={apiKey}", effectiveModel, apiKey)
                    .body(request)
                    .retrieve()
                    .body(MAP_RESPONSE);
        } catch (RestClientResponseException error) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_GATEWAY,
                    "Gemini 호출 실패: HTTP " + error.getStatusCode().value() + " " + providerStatus(error.getResponseBodyAsString()),
                    error
            );
        }
        long latencyMs = Math.max(0L, (System.nanoTime() - startedAt) / 1_000_000L);
        String output = extractOutputText(response);
        if (output == null || output.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "Gemini 응답에서 JSON text를 찾을 수 없습니다.");
        }
        return new LlmResponseResult(
                output.trim(),
                LlmProvider.GEMINI,
                effectiveModel,
                blankToNull(requestedReasoningEffort),
                latencyMs,
                usageValue(response, "promptTokenCount"),
                usageValue(response, "candidatesTokenCount"),
                usageValue(response, "totalTokenCount")
        );
    }

    Map<String, Object> requestBody(
            String systemPrompt,
            String userPrompt,
            Map<String, Object> jsonSchema,
            Integer requestedMaxOutputTokens
    ) {
        Map<String, Object> generationConfig = new LinkedHashMap<>();
        generationConfig.put("responseMimeType", "application/json");
        generationConfig.put("responseSchema", geminiSchema(jsonSchema));
        if (requestedMaxOutputTokens != null && requestedMaxOutputTokens > 0) {
            generationConfig.put("maxOutputTokens", requestedMaxOutputTokens);
        }

        return MockData.map(
                "systemInstruction", MockData.map(
                        "parts", List.of(MockData.map("text", systemPrompt))
                ),
                "contents", List.of(MockData.map(
                        "role", "user",
                        "parts", List.of(MockData.map("text", userPrompt))
                )),
                "generationConfig", generationConfig
        );
    }

    @SuppressWarnings("unchecked")
    private static Object geminiSchemaValue(Object value) {
        if (value instanceof Map<?, ?> source) {
            Map<String, Object> result = new LinkedHashMap<>();
            source.forEach((key, mapValue) -> {
                String keyName = String.valueOf(key);
                if (!"additionalProperties".equals(keyName)) {
                    result.put(keyName, geminiSchemaValue(mapValue));
                }
            });
            return result;
        }
        if (value instanceof List<?> list) {
            return list.stream()
                    .map(GeminiGenerateContentClient::geminiSchemaValue)
                    .toList();
        }
        return value;
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> geminiSchema(Map<String, Object> jsonSchema) {
        Object converted = geminiSchemaValue(jsonSchema);
        if (converted instanceof Map<?, ?> map) {
            Map<String, Object> result = new LinkedHashMap<>();
            map.forEach((key, value) -> result.put(String.valueOf(key), value));
            return result;
        }
        return Map.of();
    }

    @SuppressWarnings("unchecked")
    private static String extractOutputText(Map<String, Object> response) {
        Object candidates = response == null ? null : response.get("candidates");
        if (!(candidates instanceof List<?> candidateList) || candidateList.isEmpty()) {
            return null;
        }
        Object first = candidateList.getFirst();
        if (!(first instanceof Map<?, ?> candidate)) {
            return null;
        }
        Object content = candidate.get("content");
        if (!(content instanceof Map<?, ?> contentMap)) {
            return null;
        }
        Object parts = contentMap.get("parts");
        if (!(parts instanceof List<?> partList)) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (Object part : partList) {
            if (part instanceof Map<?, ?> partMap && partMap.get("text") instanceof String text) {
                builder.append(text);
            }
        }
        return builder.isEmpty() ? null : builder.toString();
    }

    @SuppressWarnings("unchecked")
    private static Integer usageValue(Map<String, Object> response, String key) {
        Object usage = response == null ? null : response.get("usageMetadata");
        if (!(usage instanceof Map<?, ?> usageMap)) {
            return null;
        }
        Object value = usageMap.get(key);
        if (value instanceof Number number) {
            return number.intValue();
        }
        return null;
    }

    private static String providerStatus(String responseBody) {
        if (responseBody == null || responseBody.isBlank()) {
            return "";
        }
        if (responseBody.contains("RESOURCE_EXHAUSTED")) {
            return "RESOURCE_EXHAUSTED";
        }
        if (responseBody.contains("NOT_FOUND")) {
            return "NOT_FOUND";
        }
        if (responseBody.contains("INVALID_ARGUMENT")) {
            return "INVALID_ARGUMENT";
        }
        if (responseBody.contains("PERMISSION_DENIED")) {
            return "PERMISSION_DENIED";
        }
        return "";
    }

    private static String normalizeModel(String value) {
        String model = blankToNull(value) == null ? "gemini-2.5-flash" : value.trim();
        if (model.startsWith("models/")) {
            return model.substring("models/".length());
        }
        return model;
    }

    private static String trimTrailingSlash(String value) {
        String trimmed = value == null || value.isBlank()
                ? "https://generativelanguage.googleapis.com/v1beta"
                : value.trim();
        return trimmed.endsWith("/") ? trimmed.substring(0, trimmed.length() - 1) : trimmed;
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value.trim();
    }
}
