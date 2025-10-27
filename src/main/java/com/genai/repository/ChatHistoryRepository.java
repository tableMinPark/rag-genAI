package com.genai.repository;

import com.genai.service.domain.ChatHistory;
import com.genai.service.domain.RagChatHistory;

public interface ChatHistoryRepository {

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
