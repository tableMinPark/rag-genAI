package com.genai.core.repository.request;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.genai.core.repository.vo.SearchPageVO;
import com.genai.core.repository.vo.SearchSortVO;
import com.genai.core.repository.vo.SearchTraceInfoVO;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class KeywordSearchRequest {

    private final int searchMode;

    private final int useLa;

    @JsonInclude(JsonInclude.Include.NON_NULL)
    private final String filterQuery;

    private final List<String> searchField;

    private final String commonQuery;

    private final SearchPageVO paging;

    private final List<SearchSortVO> sorting;

    private final SearchTraceInfoVO traceInfo;

    private final List<String> sessionInfo;

    private final int synonymExpansion;

    private final int useSynonym;

    private final boolean hideQueryLog;

    @Builder
    public KeywordSearchRequest(String filterQuery, int topK, List<String> searchField, String commonQuery, List<String> sessionInfo, List<SearchSortVO> sorting, boolean hideQueryLog, int useSynonym, int synonymExpansion) {
        this.searchMode = 1;
        this.useLa = 1;
        this.filterQuery = filterQuery;
        this.searchField = searchField;
        this.commonQuery = commonQuery;
        this.paging = new SearchPageVO(0, topK);
        this.sessionInfo = sessionInfo;
        this.sorting = sorting;
        this.traceInfo = new SearchTraceInfoVO(true, 0.0);
        this.hideQueryLog = hideQueryLog;
        this.useSynonym = useSynonym;
        this.synonymExpansion = synonymExpansion;
    }
}
