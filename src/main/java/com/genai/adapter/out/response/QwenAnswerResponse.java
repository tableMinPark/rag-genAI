package com.genai.adapter.out.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Collections;
import java.util.List;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class QwenAnswerResponse {

    private String id;

    private String object;

    private Integer created;

    private String model;

    private List<Choice> choices = Collections.emptyList();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Choice {

        private Integer index;

        private Delta delta;

        @JsonProperty("logprobs")
        private String logProbs;

        @JsonProperty("stop_reason")
        private String stopReason;

        @JsonProperty("finish_reason")
        private String finishReason;
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Delta {

        private String role;

        private String content;

        @JsonProperty("reasoning_content")
        private String reasoningContent;

        /**
         * 공백 문자 치환
         * SSE Event 수신 시, 공백 문자 누락 이슈 발생
         */
        public String getContent() {
            String content = this.content;

            if (this.content != null) {
                content = content.replace(" ", "&nbsp");
                content = content.replace("\n", "\\n");
            }

            return content;
        }

        /**
         * 공백 문자 치환
         * SSE Event 수신 시, 공백 문자 누락 이슈 발생
         */
        public String getReasoningContent() {
            String reasoningContent = this.reasoningContent;

            if (this.reasoningContent != null) {
                reasoningContent = reasoningContent.replace(" ", "&nbsp");
                reasoningContent = reasoningContent.replace("\n", "\\n");
            }

            return reasoningContent;
        }
    }
}
