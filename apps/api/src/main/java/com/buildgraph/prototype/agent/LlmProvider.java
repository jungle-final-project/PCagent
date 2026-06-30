package com.buildgraph.prototype.agent;

public enum LlmProvider {
    OPENAI("openai"),
    GEMINI("gemini");

    private final String storageValue;

    LlmProvider(String storageValue) {
        this.storageValue = storageValue;
    }

    public String storageValue() {
        return storageValue;
    }
}
