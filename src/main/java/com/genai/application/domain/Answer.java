package com.genai.application.domain;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;

@ToString
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class Answer {

    private String id;

    private String content;

    private String finish_reason;

    /**
     * 공백 문자 치환
     * SSE Event 수신 시, 공백 문자 누락 이슈 발생
     */
    public void replaceBlackAndLine() {
        this.content = this.content.replace(" ", "&nbsp");
        this.content = this.content.replace("\n", "\\n");
    }
}
