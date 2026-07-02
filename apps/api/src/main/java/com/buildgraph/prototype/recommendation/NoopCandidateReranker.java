package com.buildgraph.prototype.recommendation;

import java.util.Map;

public class NoopCandidateReranker implements CandidateReranker {
    @Override
    public void recordShadowScores(Map<String, Object> request, Map<String, Object> response, Long userId, String requestedAiProfile) {
        // Intentionally empty. Tests and fallback paths use this to keep recommendation order unchanged.
    }
}
