package com.genai.core.service.business.subscriber;

import lombok.Builder;
import lombok.Getter;
import lombok.Setter;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@Getter
public class Stream {
    private final String streamId;
    private final SseEmitter emitter;

    @Setter
    private boolean inferenceStarted;
    @Setter
    private boolean answerStarted;
    @Setter
    private boolean cancelled;

    @Builder
    public Stream(String streamId) {
        this.streamId = streamId;
        this.emitter = new SseEmitter(Long.MAX_VALUE);
        this.cancelled = false;
        this.inferenceStarted = false;
        this.answerStarted = false;
    }
}
