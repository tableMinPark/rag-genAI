package com.genai.core.service.module;

import com.genai.core.repository.entity.ChatPassageEntity;

import java.util.List;

public interface ChatHistoryModuleService {

    /**
     * 대화 이력 저장
     *
     * @param chatId              대화 ID
     * @param chatDetailId        대화 상세 ID
     * @param answer              답변
     * @param chatPassageEntities 참고 문서 목록
     */
    void updateChatDetail(Long chatId, Long chatDetailId, String rewriteQuery, String answer, List<ChatPassageEntity> chatPassageEntities);

    /**
     * 대화 상태 업데이트
     *
     * @param chatId    대화 ID
     * @param chatState 대화 상태
     */
    void updateChatState(Long chatId, String chatState);
}
