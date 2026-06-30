package com.buildgraph.prototype.agent;

import static org.assertj.core.api.Assertions.assertThat;

import com.buildgraph.prototype.common.MockData;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class GeminiGenerateContentClientTest {
    @Test
    void structuredJsonRequestUsesGenerationConfigSchema() {
        GeminiGenerateContentClient client = new GeminiGenerateContentClient(
                "https://generativelanguage.googleapis.com/v1beta",
                "test-api-key"
        );
        Map<String, Object> schema = MockData.map(
                "type", "object",
                "required", List.of("assistantMessage"),
                "properties", MockData.map(
                        "assistantMessage", MockData.map("type", "string")
                ),
                "additionalProperties", false
        );

        Map<String, Object> request = client.requestBody("system", "user", schema, 900);

        @SuppressWarnings("unchecked")
        Map<String, Object> systemInstruction = (Map<String, Object>) request.get("systemInstruction");
        assertThat(systemInstruction).isNotNull();
        assertThat(request.get("contents")).isInstanceOf(List.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> generationConfig = (Map<String, Object>) request.get("generationConfig");
        assertThat(generationConfig).containsEntry("responseMimeType", "application/json");
        assertThat(generationConfig).containsEntry("maxOutputTokens", 900);

        @SuppressWarnings("unchecked")
        Map<String, Object> responseSchema = (Map<String, Object>) generationConfig.get("responseSchema");
        assertThat(responseSchema).containsEntry("type", "object");
        assertThat(responseSchema).doesNotContainKey("additionalProperties");
    }
}
