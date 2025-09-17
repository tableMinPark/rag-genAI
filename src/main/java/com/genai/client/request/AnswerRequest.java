package com.genai.client.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
@AllArgsConstructor
public class AnswerRequest {

    @JsonProperty("prmpt")
    private final String prompt;

    @JsonProperty("model_name")
    private final String modelName;

    @JsonProperty("user_id")
    private final String userId;

    @JsonProperty("user_input")
    private final String userInput;

    @JsonProperty("temperature")
    private final double temperature;

    @JsonProperty("top_p")
    private final double topP;

    @JsonProperty("max_tokens")
    private final int maxTokens;

    @JsonProperty("multi_turn")
    private final boolean multiTurn;

    @JsonProperty("stream")
    private final boolean stream;

    @JsonProperty("context")
    private final String context;

    public AnswerRequest(String prompt, String modelName, String userId, String userInput, String context, boolean stream) {
        this.prompt = prompt;
        this.modelName = modelName;
        this.userId = userId;
        this.userInput = userInput;
        this.temperature = 0.01;
        this.topP = 0.9;
        this.maxTokens = 4096;
        this.multiTurn = false;
        this.stream = stream;
        this.context = context;
    }
}
