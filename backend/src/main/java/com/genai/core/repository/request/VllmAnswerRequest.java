package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.genai.core.repository.vo.ConversationVO;
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
    public VllmAnswerRequest(String modelName, double temperature, double topP, double minP, double topK, int maxTokens, boolean stream,
                             String prompt, String chatState, List<ConversationVO> conversations, String context, String query) {
        this.stream = stream;
        this.maxTokens = maxTokens;
        this.topP = topP;
        this.minP = minP;
        this.topK = topK;
        this.temperature = temperature;
        this.model = modelName;
        this.messages = new ArrayList<>();

        // 시스템 프롬프트
        this.messages.add(Message.builder().role("system").content(prompt).build());
        // 장기 기억 (이전 대화 요약)
        if (chatState != null && !chatState.isBlank()) {
            this.messages.add(Message.builder().role("system").content("이전 대화 요약:\n" + chatState).build());
        }
        if (context != null && !context.isBlank()) {
            this.messages.add(Message.builder().role("user").content("참고 문서:\n\n```" + context + "```").build());
        }
        // 이전 대화 목록
        if (conversations != null && !conversations.isEmpty()) {
            StringBuilder conversionsBuilder = new StringBuilder();
            for (int index = 0; index < conversations.size(); index++) {
                ConversationVO conversation = conversations.get(index);
                conversionsBuilder.append("Q").append(index).append(": ").append(conversation.getQuery()).append("\n");
                conversionsBuilder.append("A").append(index).append(": ").append(conversation.getAnswer()).append("\n");
            }
            this.messages.add(Message.builder().role("system").content("이전 대화 내역:\n" + conversionsBuilder.toString().trim()).build());
        }
        if (query != null && !query.isBlank()) {
            this.messages.add(Message.builder().role("user").content(query).build());
        }
    }

    @Builder
    @ToString
    @Getter
    @AllArgsConstructor
    public static class Message {

        private final String role;

        private final String content;
    }
}
