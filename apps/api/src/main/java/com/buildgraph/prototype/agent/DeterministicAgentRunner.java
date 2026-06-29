package com.buildgraph.prototype.agent;

import org.springframework.transaction.annotation.Transactional;

public class DeterministicAgentRunner implements AgentRunner {
    private final AgentTraceService agentTraceService;
    private final AgentRagRetrievalService agentRagRetrievalService;

    public DeterministicAgentRunner(AgentTraceService agentTraceService, AgentRagRetrievalService agentRagRetrievalService) {
        this.agentTraceService = agentTraceService;
        this.agentRagRetrievalService = agentRagRetrievalService;
    }

    @Override
    @Transactional
    public void run(String sessionId, AgentSessionRoot root, AgentRunProfile profile) {
        AgentRagEvidenceDraft evidence = agentRagRetrievalService.retrieveEvidence(root, profile);
        agentTraceService.recordRagEvidence(sessionId, evidence);
        agentTraceService.advanceStatus(sessionId, AgentStatus.RAG_SEARCHED, "SYSTEM", "RAG evidence retrieved for " + profile.purpose());

        for (AgentToolInvocationDraft draft : AgentRunTraceDrafts.toolInvocations(root, profile)) {
            agentTraceService.recordToolInvocation(sessionId, draft);
        }
        agentTraceService.advanceStatus(sessionId, AgentStatus.TOOLS_CALLED, "SYSTEM", "tool invocations completed for " + profile.purpose());

        agentTraceService.updateSummary(sessionId, AgentRunTraceDrafts.deterministicSummary(profile, evidence));
        agentTraceService.advanceStatus(sessionId, AgentStatus.SUMMARY_READY, "SYSTEM", "summary generated for " + profile.summaryTarget());
        agentTraceService.advanceStatus(sessionId, AgentStatus.SUCCEEDED, "SYSTEM", "agent run completed");
    }
}
