package com.genai.core.service.business.vo;

import com.genai.core.constant.StreamConst;
import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StreamEventVO {

    private String id;

    private String content;

    private StreamConst.Event event;

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
