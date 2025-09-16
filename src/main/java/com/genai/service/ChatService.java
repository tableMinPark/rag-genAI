package com.genai.service;

import com.genai.client.ModelClient;
import com.genai.client.SearchClient;
import com.genai.client.response.RerankResponse;
import com.genai.client.response.SearchResponse;
import com.genai.client.vo.DocumentVo;
import com.genai.constant.ChatConst;
import com.genai.constant.SearchConst;
import com.genai.entity.LawEntity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

@Slf4j
@Service
@RequiredArgsConstructor
public class ChatService {

    private final SearchClient searchClient;

    private final ModelClient modelClient;

    /**
     * 사용자 질의 후 실시간 답변 스트림 생성
     *
     * @param query     질의문
     * @param sessionId 세션 식별자
     */
    public void questionStream(String query, String sessionId, Consumer<String> callback) {

        // 키워드 검색
        SearchResponse<LawEntity> keywordSearch = searchClient.lawKeywordSearch(query, SearchConst.KEYWORD_TOP_K, sessionId);

        // 벡터 검색
        SearchResponse<LawEntity> vectorSearch = searchClient.lawVectorSearch(query, SearchConst.VECTOR_TOP_K);

        List<DocumentVo<LawEntity>> documents = new ArrayList<>(keywordSearch.getDocument());
        documents.addAll(vectorSearch.getDocument());

        // 검색 결과 리랭킹
        RerankResponse<LawEntity> rerank = modelClient.lawRerank(query, documents);

        // Context 생성
        StringBuilder contextBuilder = new StringBuilder();
        rerank.getData().stream()
                .filter(document -> document.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .forEach(document -> contextBuilder.append(document.getFields().getContext()).append("\n"));

        log.info("\n[ Context 토큰 수: {} ]\n{}", contextBuilder.length(), contextBuilder.toString().trim());

        // TODO: 프롬 프트 조회 기능 구현 필요 (DB)
        String prompt = "사실인 내용만 답변";

        modelClient.generateStreamAnswer(query, contextBuilder.toString().trim(), prompt, sessionId)
                .subscribe(
                        answerResponse -> answerResponse.getData().forEach(answerVo -> callback.accept(answerVo.getContent())),
                        error -> callback.accept(ChatConst.STREAM_OVER_PREFIX),
                        () -> callback.accept(ChatConst.STREAM_OVER_PREFIX));
    }
}
