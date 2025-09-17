package com.genai.client.request;

import com.genai.client.vo.PageVo;
import com.genai.client.vo.SortVo;
import com.genai.client.vo.TraceInfoVo;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
@AllArgsConstructor
public class KeywordSearchRequest {

    private final int searchMode;

    private final int useLa;

    private final List<String> searchField;

    private final String commonQuery;

    private final PageVo paging;

    private final List<SortVo> sorting;

    private final TraceInfoVo traceInfo;

    private final List<String> sessionInfo;

    private final int synonymExpansion;

    private final int useSynonym;

    private final boolean hideQueryLog;

    /**
     * 키워드 검색 요청 생성자
     * @param query 질의문
     * @param topK TOP K
     * @param sessionId 세션 식별자
     */
    public KeywordSearchRequest(String query, int topK, String sessionId) {
        this.searchMode = 1;
        this.useLa = 1;
        this.searchField = List.of("context");
        this.commonQuery = query;
        this.paging = new PageVo(0, topK);
        this.sorting = List.of(new SortVo("score", false));
        this.traceInfo = new TraceInfoVo(true, 0.0);
        this.sessionInfo = List.of(sessionId);
        this.synonymExpansion = 0;
        this.useSynonym = 0;
        this.hideQueryLog = false;
    }
}
