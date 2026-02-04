package com.genai.core.service.business.subscriber;

import com.genai.core.service.business.constant.StreamCoreConst;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.BaseSubscriber;

import java.io.IOException;
import java.util.List;

@Slf4j
public class StreamSubscriber extends BaseSubscriber<StreamEvent> {

    private final Stream stream;
    private StreamCoreConst.Event currentEvent = StreamCoreConst.Event.INITIALIZE;

    public StreamSubscriber(Stream stream) {
        this.stream = stream;
    }

    public SseEmitter getEmitter() {
        return stream.getEmitter();
    }

    public void cancelStream() {
        stream.setCancelled(true);
    }

    @Override
    protected void hookOnNext(@NonNull StreamEvent streamEvent) {
        if (stream.isCancelled()) {
            cancel();
            return;
        }

        try {
            synchronized (StreamSubscriber.this) {
                StreamCoreConst.Event nextEvent = streamEvent.getEvent();

                if (!currentEvent.equals(nextEvent)) {
                    // 이전 이벤트 종료 신호 전송
                    if (!StreamCoreConst.Event.INITIALIZE.equals(currentEvent)) {
                        stream.getEmitter().send(SseEmitter.event()
                                .name(currentEvent.done)
                                .data(currentEvent.done));
                    }
                    stream.getEmitter().send(SseEmitter.event()
                            .name(nextEvent.start)
                            .data(nextEvent.start));

                    currentEvent = nextEvent;
                }
            }

            String content = streamEvent.getConvertContent();

            if (!content.isBlank()) {
                stream.getEmitter().send(SseEmitter.event()
                        .name(currentEvent.process)
                        .data(content));
            }
        } catch (IllegalStateException | IOException ignored) {
        }
    }

    @Override
    protected void hookOnCancel() {
        try {
            List<StreamCoreConst.Event> doneEvents = StreamCoreConst.EVENT_STEP.stream()
                    .filter(event -> !StreamCoreConst.Event.INITIALIZE.equals(event))
                    .filter(event -> event.sort >= currentEvent.sort)
                    .toList();

            for (StreamCoreConst.Event event : doneEvents) {
                stream.getEmitter().send(SseEmitter.event()
                        .name(event.done)
                        .data(event.done));
            }

            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamCoreConst.DISCONNECT)
                    .data(StreamCoreConst.DISCONNECT));

        } catch (IllegalStateException | IOException ignored) {
        }

        log.warn("답변 스트림 중지");
        stream.getEmitter().complete();
    }

    @Override
    protected void hookOnError(@NonNull Throwable throwable) {
        try {
            List<StreamCoreConst.Event> doneEvents = StreamCoreConst.EVENT_STEP.stream()
                    .filter(event -> !StreamCoreConst.Event.INITIALIZE.equals(event))
                    .filter(event -> event.sort >= currentEvent.sort)
                    .toList();

            for (StreamCoreConst.Event event : doneEvents) {
                stream.getEmitter().send(SseEmitter.event()
                        .name(event.done)
                        .data(event.done));
            }

            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamCoreConst.EXCEPTION)
                    .data(throwable.getMessage()));

            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamCoreConst.DISCONNECT)
                    .data(StreamCoreConst.DISCONNECT));

        } catch (IllegalStateException | IOException ignored) {
        }

        log.error("답변 스트림 비정상 종료 : {}", throwable.getMessage());
        stream.getEmitter().completeWithError(throwable);
    }

    @Override
    protected void hookOnComplete() {
        try {
            if (!StreamCoreConst.Event.INITIALIZE.equals(currentEvent)) {
                stream.getEmitter().send(SseEmitter.event()
                        .name(currentEvent.done)
                        .data(currentEvent.done));
            }

            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamCoreConst.DISCONNECT)
                    .data(StreamCoreConst.DISCONNECT));
        } catch (IllegalStateException | IOException ignored) {
        }

        log.info("답변 스트림 정상 종료");
        stream.getEmitter().complete();
    }
}