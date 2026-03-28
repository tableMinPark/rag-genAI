package com.genai.core.service.business.constant;

import java.util.List;

public class QuestionCoreConst {

    // 멀티턴 참고 대화 이력 수
    public static final int     MULTITURN_TURN_CONVERSATION_COUNT = 5;
    public static final int     MULTITURN_TURNS                   = 3;

    // 검색 결과 상위 카운트 옵션
    public static final double  SEARCH_SCORE_MIN  = 0.2;
    public static final int     KEYWORD_TOP_K     = 10;
    public static final int     VECTOR_TOP_K      = 10;
    public static final int     RERANK_TOP_K      = 3;


    /*
     * #######################################
     * 답변 불가 프롬프트
     * #######################################
     */
    public static final String INVALID_ANSWER_PROMPT = """
    # 최우선 답변 원칙(Response Rules)
    - 참고 문서에 질문과 직접적으로 관련된 정보가 없는 경우, 아래 문구로만 답변한다.
        > **"관련 문서를 찾을 수 없습니다. 관련된 질문만 답변이 가능해요"**
    """;

    public static final List<String> REFERENCE_VALID_PATTERN = List.of(
            "관련(\\s)?s문서를(\\s)?s찾을(\\s)?수(\\s)?없습니다.",
            "관련된(\\s)?질문만(\\s)?답변이(\\s)?가능해요"
    );
}
