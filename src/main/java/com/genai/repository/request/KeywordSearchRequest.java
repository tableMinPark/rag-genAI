package com.genai.repository.request;

import com.genai.repository.vo.PageVo;
import com.genai.repository.vo.SortVo;
import com.genai.repository.vo.TraceInfoVo;
import lombok.Builder;
import lombok.Getter;
import lombok.ToString;

import java.util.List;

@ToString
@Getter
public class KeywordSearchRequest {

    private final int searchMode;

    private final int useLa;

    private final String filterQuery;

    private final List<String> searchField;

    private final String commonQuery;

    private final PageVo paging;

    private final List<SortVo> sorting;

    private final TraceInfoVo traceInfo;

    private final List<String> sessionInfo;

    private final int synonymExpansion;

    private final int useSynonym;

    private final boolean hideQueryLog;

    @Builder
    public KeywordSearchRequest(String filterQuery, int topK, List<String> searchField, String commonQuery, List<String> sessionInfo, List<SortVo> sorting, boolean hideQueryLog, int useSynonym, int synonymExpansion) {
        this.searchMode = 1;
        this.useLa = 1;
        this.filterQuery = filterQuery ==  null ? "" : filterQuery;
        this.searchField = searchField;
        this.commonQuery = commonQuery;
        this.paging = new PageVo(0, topK);
        this.sessionInfo = sessionInfo;
        this.sorting = sorting;
        this.traceInfo = new TraceInfoVo(true, 0.0);
        this.hideQueryLog = hideQueryLog;
        this.useSynonym = useSynonym;
        this.synonymExpansion = synonymExpansion;
    }
}
