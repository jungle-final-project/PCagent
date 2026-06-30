package com.buildgraph.prototype.agent;

public record AiProfileDefinition(
        AiProfile profile,
        String model,
        String reasoningEffort,
        int ragTopK,
        String promptVersion,
        int maxOutputTokens,
        int recentMessageLimit,
        boolean includeEvidenceChunkText,
        boolean includeToolResultPayload,
        boolean useCompactPrompt
) {
}
