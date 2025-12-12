package com.genai.core.service.impl;

import com.genai.core.service.StreamCoreService;
import com.genai.core.service.subscriber.StreamSubscriber;
import com.genai.core.service.vo.StreamVO;
import com.genai.core.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

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

        SseEmitter emitter = new SseEmitter(Long.MAX_VALUE);
        StreamSubscriber streamSubscriber = new StreamSubscriber(StreamVO.builder()
                .streamId(streamId)
                .emitter(emitter)
                .build());

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
            StreamVO streamVO = streamMap.get(streamId).getStream();

            // 스트림 종료
            streamVO.setCancelled(true);

            streamMap.remove(streamId);

            log.info("스트림 삭제 : {} | ({})", streamId, streamMap.size());
        }
    }
}
