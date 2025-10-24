package com.genai.application.port;

import com.genai.application.domain.ChatHistory;
import com.genai.application.domain.RagChatHistory;

public interface ChatHistoryPersistencePort {

    /**
     * RAG 질답 내역 등록
     *
     * @param chatHistory 사용자 질답 도메인 객체
     */
    void registerRagChatHistory(RagChatHistory chatHistory);

    /**
     * 사용자 질답 내역 등록
     *
     * @param chatHistory 사용자 질답 도메인 객체
     */
    void registerChatHistory(ChatHistory chatHistory);
}
