package com.genai.core.service.business.impl;

import com.genai.core.service.business.constant.StreamCoreConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.service.business.StreamCoreService;
import com.genai.core.service.business.subscriber.StreamSubscriber;
import com.genai.core.service.business.subscriber.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Service
@RequiredArgsConstructor
public class StreamCoreServiceImpl implements StreamCoreService {

    private static final Map<String, StreamSubscriber> streamMap = new ConcurrentHashMap<>();

    /**
     * Stream 등록
     *
     * @param streamId stream 식별자
     * @return StreamVO
     */
    @Override
    public StreamSubscriber createStream(String streamId) {

        StreamSubscriber streamSubscriber = new StreamSubscriber(Stream.builder()
                .streamId(streamId)
                .build());

        // 연결 이벤트 전송
        try {
            streamSubscriber.getEmitter().send(SseEmitter.event()
                    .name(StreamCoreConst.CONNECT)
                    .data(StreamCoreConst.CONNECT));
        } catch (IOException e) {
            streamSubscriber.getEmitter().completeWithError(e);
        }

        streamMap.put(streamId, streamSubscriber);

        log.info("스트림 등록 : {} | ({})", streamId, streamMap.size());

        return streamSubscriber;
    }

    /**
     * Stream 조회
     *
     * @param streamId stream 식별자
     * @return StreamVO
     */
    @Override
    public StreamSubscriber getStream(String streamId) {
        if (streamMap.containsKey(streamId)) {
            log.info("스트림 조회 : {} | ({})", streamId, streamMap.size());
            return streamMap.get(streamId);
        }

        throw new NotFoundException(streamId + " 스트림");
    }

    /**
     * Stream 중지 및 삭제
     *
     * @param streamId stream 식별자
     */
    public void deleteStream(String streamId) {
        if (streamMap.containsKey(streamId)) {
            // 스트림 종료
            streamMap.get(streamId).cancelStream();
            streamMap.remove(streamId);

            log.info("스트림 삭제 : {} | ({})", streamId, streamMap.size());
        }
    }
}
