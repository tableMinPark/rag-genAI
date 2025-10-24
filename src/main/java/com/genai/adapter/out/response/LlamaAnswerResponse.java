package com.genai.adapter.out.response;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Collections;
import java.util.List;

@ToString
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class LlamaAnswerResponse {

    private String status;

    private String message;

    private List<Data> data = Collections.emptyList();

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Data {

        private String id;

        private String content;

        @JsonProperty("finish_reason")
        private String finishReason;

        /**
         * 공백 문자 치환
         * SSE Event 수신 시, 공백 문자 누락 이슈 발생
         */
        public String getContent() {
            if (this.content == null) {
                return "";
            }
            String content = this.content;
            content = content.replace(" ", "&nbsp");
            content = content.replace("\n", "\\n");
            return content;
        }
    }
}
