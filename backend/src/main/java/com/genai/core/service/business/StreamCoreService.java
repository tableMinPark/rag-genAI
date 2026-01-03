package com.genai.core.service.business;

import com.genai.core.service.business.subscriber.StreamSubscriber;


public interface StreamCoreService {

    /**
     * Stream 등록
     *
     * @param streamId stream 식별자
     * @return StreamVO
     */
    StreamSubscriber createStream(String streamId);

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
