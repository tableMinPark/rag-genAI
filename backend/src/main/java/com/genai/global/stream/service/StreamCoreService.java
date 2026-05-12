package com.genai.global.stream.service;

import com.genai.global.stream.subscriber.StreamSubscriber;


public interface StreamCoreService {

    /**
     * Stream 등록
     *
     * @param streamId stream 식별자
     * @param chatId   대화 ID (connect 이벤트로 전달)
     * @return StreamVO
     */
    StreamSubscriber createStream(String streamId, long chatId);

    /**
     * Stream 등록 (chatId 없는 경우)
     *
     * @param streamId stream 식별자
     * @return StreamVO
     */
    default StreamSubscriber createStream(String streamId) {
        return createStream(streamId, 0L);
    }

    /**
     * Stream 조회
     *
     * @param streamId stream 식별자
     * @return StreamVO
     */
    StreamSubscriber getStream(String streamId);

    /**
     * Stream 중지 및 삭제
     *
     * @param streamId stream 식별자
     */
    void deleteStream(String streamId);
}