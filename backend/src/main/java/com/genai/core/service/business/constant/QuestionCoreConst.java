package com.genai.core.service.business.constant;

import java.util.List;

public class QuestionCoreConst {

    // 검색 결과 상위 카운트 옵션
    public static final double       SEARCH_SCORE_MIN  = 0.2;
    public static final int          KEYWORD_TOP_K     = 10;
    public static final int          VECTOR_TOP_K      = 10;
    public static final int          RERANK_TOP_K      = 3;

    public static final List<String> REFERENCE_VALID_PATTERN = List.of(
            "관련(\\s)?s문서를(\\s)?s찾을(\\s)?수(\\s)?없습니다.",
            "관련된(\\s)?질문만(\\s)?답변이(\\s)?가능해요"
    );

    // 멀티턴 참고 대화 이력 수
    public static final int     MULTITURN_TURNS        = 3;
}
