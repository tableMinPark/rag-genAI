package com.genai.adapter.out;

import com.genai.application.domain.*;
import com.genai.application.port.ChatHistoryPersistencePort;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ChatHistoryPersistenceAdapter implements ChatHistoryPersistencePort {

    /**
     * TODO: RAG 질답 내역 등록
     *
     * @param chatHistory 사용자 질답 도메인 객체
     */
    @Override
    public void registerRagChatHistory(RagChatHistory chatHistory) {

        List<Search<Document>> keywordSearchDocuments = chatHistory.getKeywordSearchDocuments();
        List<Search<Document>> vectorSearchDocuments = chatHistory.getVectorSearchDocuments();
        List<Rerank> reranks = chatHistory.getReranks();
        List<Rerank> topReranks = chatHistory.getTopReranks();
        String query = chatHistory.getQuery();
        String inference = chatHistory.getInference();
        String answer = chatHistory.getAnswer();
        String sessionId = chatHistory.getSessionId();

        StringBuilder keywordSearchLogBuilder = new StringBuilder();
        keywordSearchDocuments.forEach(document ->
                keywordSearchLogBuilder.append(String.format("+ %.4f", document.getScore()))
                        .append(" | ")
                        .append(document.getFields().getContext().replace("\n", "\\n"))
                        .append("\n"));
        StringBuilder vectorSearchLogBuilder = new StringBuilder();
        vectorSearchDocuments.forEach(document ->
                vectorSearchLogBuilder.append(String.format("+ %.4f", document.getScore()))
                        .append(" | ")
                        .append(document.getFields().getContext().replace("\n", "\\n"))
                        .append("\n"));
        StringBuilder rerankLogBuilder = new StringBuilder();
        reranks.forEach(document ->
                rerankLogBuilder.append(String.format("+ %.4f", document.getRerankScore()))
                        .append(" | ")
                        .append(document.getDocument().getContext().replace("\n", "\\n"))
                        .append("\n"));
        StringBuilder topRerankLogBuilder = new StringBuilder();
        topReranks.forEach(document ->
                topRerankLogBuilder.append(String.format("+ %.4f", document.getRerankScore()))
                        .append(" | ")
                        .append(document.getDocument().getContext().replace("\n", "\\n"))
                        .append("\n"));
        log.info("##### 키워드 검색 결과 {} 개 | {} #####\n{}", keywordSearchDocuments.size(), query, keywordSearchLogBuilder.toString().trim());
        log.info("##### 벡터 검색 결과 {} 개 | {} #####\n{}", vectorSearchDocuments.size(), query, vectorSearchLogBuilder.toString().trim());
        log.info("##### 리랭킹 결과 {} 개 | {} #####\n{}", reranks.size(), query, rerankLogBuilder.toString().trim());
        log.info("##### 컨텍스트 선택 결과 {} 개 | {} #####\n{}", topReranks.size(), query, topRerankLogBuilder.toString().trim());
        log.info("##### LLM 추론({}) | {} #####\n{}", sessionId, query, inference);
        log.info("##### LLM 답변({}) #####\nQ. {}\n---------------------------\n{}", sessionId, query, answer);
    }

    /**
     * TODO: 사용자 질답 내역 등록
     *
     * @param chatHistory 사용자 질답 도메인 객체
     */
    @Override
    public void registerChatHistory(ChatHistory chatHistory) {

        String query = chatHistory.getQuery();
        String inference = chatHistory.getInference();
        String answer = chatHistory.getAnswer();
        String sessionId = chatHistory.getSessionId();

        log.info("##### LLM 추론({}) | {} #####\n{}", sessionId, query, inference);
        log.info("##### LLM 답변({}) #####\nQ. {}\n---------------------------\n{}", sessionId, query, answer);
    }
}
