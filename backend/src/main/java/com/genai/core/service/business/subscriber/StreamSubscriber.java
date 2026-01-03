package com.genai.core.service.business.subscriber;

import com.genai.core.constant.StreamConst;
import com.genai.core.service.business.vo.AnswerVO;
import com.genai.core.service.business.vo.StreamVO;
import lombok.NonNull;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;
import reactor.core.publisher.BaseSubscriber;

import java.io.IOException;
import java.util.List;

@Slf4j
public class StreamSubscriber extends BaseSubscriber<List<AnswerVO>> {
    private final StreamVO stream;

    public StreamSubscriber(StreamVO stream) {
        this.stream = stream;
    }

    public SseEmitter getEmitter() {
        return stream.getEmitter();
    }

    public void cancelStream() {
        stream.setCancelled(true);
    }

    @Override
    protected void hookOnNext(@NonNull List<AnswerVO> answers) {
        if (stream.isCancelled()) {
            cancel();
            return;
        }

        try {
            for (AnswerVO answer : answers) {
                if (answer.getIsInference() != null) {
                    if (answer.getIsInference()) {
                        synchronized (StreamSubscriber.this) {
                            if (!stream.isInferenceStarted()) {
                                stream.setInferenceStarted(true);
                                // 추론 시작 이벤트 전송
                                stream.getEmitter().send(SseEmitter.event()
                                        .name(StreamConst.INFERENCE_START)
                                        .data(StreamConst.INFERENCE_START));
                            }
                            // 추론 이벤트 전송
                            stream.getEmitter().send(SseEmitter.event()
                                    .name(StreamConst.INFERENCE)
                                    .data(answer.getConvertContent()));
                        }
                    } else {
                        synchronized (StreamSubscriber.this) {
                            if (!stream.isAnswerStarted()) {
                                stream.setAnswerStarted(true);
                                // 추론 끝 이벤트 전송
                                stream.getEmitter().send(SseEmitter.event()
                                        .name(StreamConst.INFERENCE_DONE)
                                        .data(StreamConst.INFERENCE_DONE));
                                // 답변 시작 이벤트 전송
                                stream.getEmitter().send(SseEmitter.event()
                                        .name(StreamConst.ANSWER_START)
                                        .data(StreamConst.ANSWER_START));
                            }
                            // 답변 이벤트 전송
                            stream.getEmitter().send(SseEmitter.event()
                                    .name(StreamConst.ANSWER)
                                    .data(answer.getConvertContent()));
                        }
                    }
                }
            }
        } catch (IOException e) {
            log.error("답변 스트림 SSE 전송 실패 : {}", e.getMessage());
            cancel();
        }
    }

    @Override
    protected void hookOnCancel() {
        try {
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.INFERENCE_DONE)
                    .data(StreamConst.INFERENCE_DONE));
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.ANSWER_DONE)
                    .data(StreamConst.ANSWER_DONE));
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.DISCONNECT)
                    .data(StreamConst.DISCONNECT));
        } catch (IOException ignored) {
        }

        log.error("답변 스트림 중지");
        stream.getEmitter().complete();
    }

    @Override
    protected void hookOnError(@NonNull Throwable throwable) {
        try {
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.INFERENCE_DONE)
                    .data(StreamConst.INFERENCE_DONE));
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.ANSWER_DONE)
                    .data(StreamConst.ANSWER_DONE));
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.EXCEPTION)
                    .data(throwable.getMessage()));
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.DISCONNECT)
                    .data(StreamConst.DISCONNECT));
        } catch (IOException ignored) {
        }

        log.error("답변 스트림 비정상 종료 : {}", throwable.getMessage());
        stream.getEmitter().completeWithError(throwable);
    }

    @Override
    protected void hookOnComplete() {
        try {
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.ANSWER_DONE)
                    .data(StreamConst.ANSWER_DONE));
            stream.getEmitter().send(SseEmitter.event()
                    .name(StreamConst.DISCONNECT)
                    .data(StreamConst.DISCONNECT));
        } catch (IOException ignored) {
        }

        log.info("답변 스트림 정상 종료");
        stream.getEmitter().complete();
    }
}