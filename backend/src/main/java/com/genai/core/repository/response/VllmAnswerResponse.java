package com.genai.core.repository.response;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class VllmAnswerResponse implements AnswerResponse {

    private String id;

    private String object;

    private Integer created;

    private String model;

    private List<Choice> choices = Collections.emptyList();

    @Override
    public List<Data> getDatas() {
        return choices.stream()
                .map(choice -> AnswerResponse.Data.builder()
                        .reasoningContent(choice.getMessage().getReasoningContent())
                        .content(choice.getMessage().getContent())
                        .finishReason(choice.getFinishReason())
                        .build())
                .collect(Collectors.toList());
    }


    @ToString
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Choice {

        private Integer index;

        @JsonAlias({"message", "delta"})
        private Delta message;

        @JsonProperty("logprobs")
        private String logProbs;

        @JsonProperty("stop_reason")
        private String stopReason;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @ToString
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Delta {

        private String role;

        private String content;

        @JsonProperty("reasoning_content")
        private String reasoningContent;
    }
}
