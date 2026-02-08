package com.genai.core.service.module;

import com.genai.core.repository.vo.ConversationVO;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

public interface QuestionModuleService {

    /**
     * 대화 이력 목록 조회
     *
     * @param chatId 대화 ID
     * @param size   대화 수
     * @return 대화 이력 목록
     */
    Flux<ConversationVO> getConversations(long chatId, int size);

    /**
     * 질의 재작성
     *
     * @param query         질의
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 ID
     * @return 재작성 질의
     */
    Mono<String> rewriteQuery(String query, List<ConversationVO> conversations, String sessionId);

    /**
     * 대화 상태 요약 생성
     *
     * @param chatState     이전 대화 상태
     * @param conversations 대화 이력 목록
     * @param sessionId     세션 ID
     * @return 대화 상태 요약
     */
    Mono<String> summaryState(String chatState, List<ConversationVO> conversations, String sessionId);
}
