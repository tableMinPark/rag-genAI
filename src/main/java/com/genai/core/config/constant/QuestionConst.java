package com.genai.core.config.constant;

public class QuestionConst {

    // 멀티턴 참고 대화 이력 수
    public static final int    MULTITURN_TURNS              = 3;

    // 멀티턴 재질의 생성 설정
    public static final int    REWRITE_QUERY_TURNS          = 3;
    public static final double REWRITE_QUERY_TEMPERATURE    = 0.1;
    public static final double REWRITE_QUERY_TOP_P          = 0.9;
    public static final int    REWRITE_QUERY_MAXIMUM_TOKENS = 256;
    public static final String REWRITE_QUERY_PROMPT         = """
    너는 멀티턴 RAG 시스템의 "질문 재작성기" 역할이다.
    
    목표:
    - 사용자의 현재 질문을
      문서 검색에 적합한 "완결된 단일 질문"으로 재작성한다.
    
    규칙:
    1. 절대로 답변을 생성하지 말 것.
    2. 추론, 판단, 조언을 추가하지 말 것.
    3. 문서에 존재한다고 가정하거나 단정하지 말 것.
    4. 이전 대화 맥락을 참고하여 생략된 주어, 대상, 목적을 복원할 것.
    5. 출력은 검색용 질문 문장만 포함할 것.
    6. 불필요한 수식어는 제거하고 명확하게 작성할 것.
    7. 출력에 설명, 이유, 판단, 번호, 불릿, 따옴표를 포함하지 말 것.
    """;

    // 멀티턴 답변 요약 관련 설정
    public static final int    SUMMARY_UPDATE_TURNS   = 3;
    public static final double SUMMARY_UPDATE_TEMPERATURE    = 0.2;
    public static final double SUMMARY_UPDATE_TOP_P          = 0.9;
    public static final int    SUMMARY_UPDATE_MAXIMUM_TOKENS = 512;
    public static final String SUMMARY_UPDATE_PROMPT  = """
    너는 멀티턴 RAG 시스템의 "대화 요약기"이다.
    
    목표:
    - 최근 대화 내용을 바탕으로 세션의 상태를 간결하게 갱신한다.
    
    포함할 것:
    - 사용자의 질문 목적
    - 답변에서 문서로 확인된 사실
    - 문서에서 확인되지 않았다고 명시된 사항
    - 이후 질문에 영향을 줄 전제 조건
    
    제외할 것:
    - 문서의 세부 내용
    - 추론, 해석, 조언
    - 답변자의 의견
    - 일반 상식
    
    규칙:
    - 사실만 기록할 것
    - 불확실한 사항은 "확인되지 않음"으로 명시
    - 불릿 리스트 형식 유지
    - 요약은 최대 8개 불릿을 넘지 말 것.
    - 각 불릿은 한 문장을 넘지 말 것.
    - 기존 요약과 충돌 시 최신 정보를 우선
    """;
}
