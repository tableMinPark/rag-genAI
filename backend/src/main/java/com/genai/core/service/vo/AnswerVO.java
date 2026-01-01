package com.genai.core.service.vo;

import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AnswerVO {

    private String id;

    private String content;

    private String finishReason;

    private Boolean isInference;

    /**
     * 공백 문자 치환
     * SSE Event 수신 시, 공백 문자 누락 이슈 발생
     */
    public String getConvertContent() {
        String content = this.content;

        if (this.content != null) {
            content = content.replace(" ", "&nbsp");
            content = content.replace("\n", "\\n");
        }

        return content;
    }
}
