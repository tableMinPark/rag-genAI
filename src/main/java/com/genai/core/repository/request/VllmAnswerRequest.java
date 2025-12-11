package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@ToString
@Getter
public class VllmAnswerRequest {

    @JsonProperty("model")
    @ToString.Include(name = "model")
    private final String model;

    @JsonProperty("temperature")
    @ToString.Include(name = "temperature")
    private final double temperature;

    @JsonProperty("top_p")
    @ToString.Include(name = "top_p")
    private final double topP;

    @JsonProperty("min_p")
    @ToString.Include(name = "min_p")
    private final double minP;

    @JsonProperty("top_k")
    @ToString.Include(name = "top_k")
    private final double topK;

    @JsonProperty("max_tokens")
    @ToString.Include(name = "max_tokens")
    private final int maxTokens;

    @JsonProperty("stream")
    @ToString.Include(name = "stream")
    private final boolean stream;

    @JsonProperty("messages")
    @ToString.Include(name = "messages")
    private final List<Message> messages;

    @Builder
    public VllmAnswerRequest(String prompt, String modelName, String userInput, double temperature, double topP, double minP, double topK, int maxTokens, boolean stream, String context) {
        this.stream = stream;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.minP = minP;
        this.topK = topK;
        this.temperature = temperature;
        this.model = modelName;
        this.messages = new ArrayList<>();

        this.messages.add(Message.builder().role("system").content(prompt).build());
        this.messages.add(Message.builder().role("user").name("context").content(userInput).build());

        if (context != null) {
            this.messages.add(Message.builder().role("user").name("context").content(context).build());
        }
    }

    @Builder
    @ToString
    @Getter
    @AllArgsConstructor
    public static class Message {

        private final String role;

        @JsonInclude(JsonInclude.Include.NON_NULL)
        private final String name;

        private final String content;
    }
}
