package com.genai.core.service.business.subscriber;

import com.genai.core.service.business.constant.StreamCoreConst;
import com.genai.common.utils.StringUtil;
import lombok.*;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class StreamEvent {

    private String id;

    private String content;

    private StreamCoreConst.Event event;

    /**
     * 공백 문자 치환
     * SSE Event 수신 시, 공백 문자 누락 이슈 발생
     */
    public String getConvertContent() {
        String content = this.content;

        if (this.content != null) {
            content = content.replace(" ", "&nbsp;");
            content = content.replace("\n", "\\n");
        }

        return content;
    }

    public static <T> StreamEvent prepare(String id, T content) {
        return StreamEvent.builder()
                .id(id)
                .content(StringUtil.writeJson(content))
                .event(StreamCoreConst.Event.PREPARE)
                .build();
    }
}
