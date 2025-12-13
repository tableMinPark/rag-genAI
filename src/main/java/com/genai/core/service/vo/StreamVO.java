package com.genai.core.service.vo;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
public class StreamVO {
    private final String streamId;
    private final SseEmitter emitter;

    @Setter
    private boolean inferenceStarted;
    @Setter
    private boolean answerStarted;
    @Setter
    private boolean cancelled;

    @Builder
    public StreamVO(String streamId) {
        this.streamId = streamId;
        this.emitter = new SseEmitter(Long.MAX_VALUE);
        this.cancelled = false;
        this.inferenceStarted = false;
        this.answerStarted = false;
    }
}
