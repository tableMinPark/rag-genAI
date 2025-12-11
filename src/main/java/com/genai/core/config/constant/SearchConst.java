package com.genai.core.config.constant;

public class SearchConst {

    // 래링킹 설정 옵션
    public static final String       RERANK_FIELD      = "context";
    public static final double       RERANK_SCORE_MIN  = 0.05;

    // 검색 결과 상위 카운트 옵션
    public static final int          KEYWORD_TOP_K     = 5;
    public static final int          VECTOR_TOP_K      = 5;
    public static final int          RERANK_TOP_K      = 3;

    // 검색 공통 옵션
    public static final int          SYNONYM_EXPANSION = 0;
    public static final int          USE_SYNONYM       = 0;
    public static final boolean      HIDE_QUERY_LOG    = false;

}
