package com.genai.global.stream.subscriber;

import com.genai.global.common.utils.StringUtil;
import com.genai.global.stream.constant.StreamCoreConst;
import com.genai.global.stream.service.vo.PrepareVO;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

@ToString
@Getter
public class StreamEvent {

    private final String id;

    private final String content;

    private final StreamCoreConst.Event event;

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

    @Builder
    private StreamEvent(String id, String content, StreamCoreConst.Event event) {
        this.id = id;
        this.content = content;
        this.event = event;
    }

    public static StreamEvent prepare(String id, float progress, String message) {
        return StreamEvent.builder()
                .id(id)
                .content(StringUtil.writeJson(PrepareVO.builder()
                        .progress(progress)
                        .message(message)
                        .build()))
                .event(StreamCoreConst.Event.PREPARE)
                .build();
    }

    public static <T> StreamEvent inference(String id, T content) {
        return StreamEvent.builder()
                .id(id)
                .content(content instanceof String con  ? con : StringUtil.writeJson(content))
                .event(StreamCoreConst.Event.INFERENCE)
                .build();
    }

    public static <T> StreamEvent answer(String id, T content) {
        return StreamEvent.builder()
                .id(id)
                .content(content instanceof String con  ? con : StringUtil.writeJson(content))
                .event(StreamCoreConst.Event.ANSWER)
                .build();
    }

    public static <T> StreamEvent reference(String id, T content) {
        return StreamEvent.builder()
                .id(id)
                .content(content instanceof String con  ? con : StringUtil.writeJson(content))
                .event(StreamCoreConst.Event.REFERENCE)
                .build();
    }
}
