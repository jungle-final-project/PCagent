package com.buildgraph.prototype.recommendation;

import java.util.Map;

public interface CandidateReranker {
    void recordShadowScores(Map<String, Object> request, Map<String, Object> response, Long userId, String requestedAiProfile);
}
