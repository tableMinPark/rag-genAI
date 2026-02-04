package com.genai.core.service.module.constant;

public class QuestionModuleConst {

    // 질의 재정의 설정
    public  static final double REWRITE_QUERY_TEMPERATURE    = 0.1;
    public  static final double REWRITE_QUERY_TOP_P          = 0.9;
    private static final int    REWRITE_QUERY_MAXIMUM_TOKENS = 256;
    public  static final String REWRITE_QUERY_PROMPT         = String.format("""
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
    8. 출력 분량은 %d 토큰 이내로 작성하세요.
    """, REWRITE_QUERY_MAXIMUM_TOKENS);

    // 답변 상태 설정
    public  static final double CHAT_STATE_UPDATE_TEMPERATURE    = 0.2;
    public  static final double CHAT_STATE_UPDATE_TOP_P          = 0.9;
    private static final int    CHAT_STATE_UPDATE_MAXIMUM_TOKENS = 512;
    public  static final String CHAT_STATE_UPDATE_PROMPT = String.format("""
    You are a conversation state manager.
    
    Your task is to update the conversation state summary.
    
    Input:
    - Previous Conversation State Summary
    - Latest user question
    - Latest assistant answer
    
    Output format (keep exactly):
    - User Goal:
    - Constraints:
    - Decisions Made:
    - Open Questions:
    
    Rules:
    - Only include information explicitly stated.
    - Do NOT infer or guess.
    - Preserve previous content unless it is clearly updated.
    - If nothing changes, return the previous summary unchanged.
    - This summary is authoritative and higher priority than retrieved documents.
    - Max output token is %d.
    """, CHAT_STATE_UPDATE_MAXIMUM_TOKENS);
}
