package com.genai.core.service.business.subscriber;

import com.genai.core.service.business.constant.StreamCoreConst;
import com.genai.core.utils.ReactiveLogUtil;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.BaseSubscriber;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

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

    public void subscribeWithTrace(Flux<StreamEvent> streamFlux) {
        subscribeWithTrace(streamFlux, null);
    }

    /**
     * 로그 기반 구독 시작
     */
    public void subscribeWithTrace(Flux<StreamEvent> streamFlux, Mono<Void> streamEndMono) {
        if (streamEndMono == null) {
            streamFlux
                    .contextWrite(ctx -> ctx.put(ReactiveLogUtil.TRACE_ID_KEY, stream.getStreamId()))
                    .subscribe(this);
        } else {
            streamFlux
                    .doOnComplete(() -> streamEndMono
                            .contextWrite(ctx -> ctx.put(ReactiveLogUtil.TRACE_ID_KEY, stream.getStreamId()))
                            .subscribeOn(Schedulers.boundedElastic())
                            .subscribe())
                    .contextWrite(ctx -> ctx.put(ReactiveLogUtil.TRACE_ID_KEY, stream.getStreamId()))
                    .subscribe(this);
        }
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

        log.warn("[{}] " + String.format("%-20s", "Stream cancel") + " |", stream.getStreamId());
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

        } catch (IllegalStateException | IOException ignored) {
        }

        log.error("[{}] " + String.format("%-20s", "Stream complete with error") + " |", stream.getStreamId(), throwable);
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

        log.info("[{}] " + String.format("%-20s", "Stream complete") + " |", stream.getStreamId());
        stream.getEmitter().complete();
    }
}