package com.genai.core.service.module;

import com.genai.core.service.module.vo.ConversationVO;
import com.genai.core.service.module.vo.MultiturnConversationVO;
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
    Mono<List<ConversationVO>> getConversations(long chatId, int size);

    /**
     * 질의 재작성
     *
     * @param query         질의
     * @param conversations 대화 이력 목록
     * @return 재작성 질의
     */
    Mono<String> generateRewriteQuery(String query, List<ConversationVO> conversations);

    /**
     * 대화 상태 요약 생성
     *
     * @param query         질의문
     * @param conversations 대화 이력 목록
     * @return 대화 상태 요약
     */
    Mono<String> generateChatState(String query, List<ConversationVO> conversations);

    /**
     * 멀티턴 여부 확인
     *
     * @param query         질의문
     * @param chatState     이전 대화 상태 (주제)
     * @param conversations 대화 이력 목록
     * @return 멀티턴 여부
     */
    Mono<MultiturnConversationVO> validMultiturn(String query, String chatState, List<ConversationVO> conversations);
}
