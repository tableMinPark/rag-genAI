package com.genai.core.service.module.constant;

public class QuestionModuleConst {

    // 질의 재정의 설정
    public  static final double REWRITE_QUERY_TEMPERATURE    = 0.1;
    public  static final double REWRITE_QUERY_TOP_P          = 0.9;
    private static final int    REWRITE_QUERY_MAXIMUM_TOKENS = 512;
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
    public  static final String CHAT_STATE_UPDATE_PROMPT = """
    당신의 역할은 하나의 대화 주제(topic)가 종료될 때
    해당 주제를 요약하여 장기 기억으로 저장할 수 있는 정보를 생성하는 것이다.
    
    입력으로 제공되는 대화 이력은
    하나의 동일한 주제에 대해 진행된 사용자 질문과 시스템 답변이다.
    
    현재 새로운 질문은 포함되지 않는다.
    
    ## 입력 항목
    - 사용자가 작성한 질문 문자열
    
    ## 작업
    
    대화 전체를 분석하여 하나의 "완결된 토픽"을 생성하라.
    
    ### 토픽 제목
    
    대화를 대표하는 간결한 제목 생성
    
    규칙
    - 최대 80자
    - 기술명 또는 문제 상황 포함
    - 명사 중심
    
    ### 토픽 요약
    
    사용자가 해결하려던 문제 또는 목적을 요약
    
    규칙
    - 최대 200자
    - 1~2 문장
    - 핵심 문제 중심
    
    ### 주요 키워드
    
    토픽을 대표하는 기술 또는 개념 추출
    
    규칙
    - 최대 5개
    - 각 30자 이하
    - 기술명 중심
    
    ## 출력 규칙
    
    JSON만 출력한다.
    
    {
      "topic_title": "...",
      "topic_summary": "...",
      "keywords": ["...", "..."]
    }
    """;

    // 멀티턴 여부 판별 설정
    public static final double VALID_MULTITURN_TEMPERATURE    = 0.1;
    public static final double VALID_MULTITURN_TOP_P          = 0.9;
    public static final String VALID_MULTITURN_PROMPT         = """
    당신의 역할은 현재 사용자 질의와 이전 대화 이력을 분석하여,
    현재 질문을 이해하는 데 필요한 이전 대화 이력을 선택하고
    현재 질문이 기존 대화 주제를 이어가는지 또는 새로운 주제인지 판단하는 것이다.
    
    단순 키워드 일치가 아니라 대화의 의미와 맥락을 기반으로 판단해야 한다.
    
    # 입력 정보
    
    다음 정보가 제공된다.
    
    1. Current Query (사용자 현재 질의)
    
    2. Previous Conversations (이전 대화 이력)
       - ID (대화 이력 ID)
       - Q (이전 사용자 질의)
       - A (이전 시스템 답변)
    
    3. Conversation State (현재 저장된 토픽)
    
    # 작업
    
    다음 두 가지 작업을 수행하라.
    
    1. 현재 사용자 질의를 이해하기 위해 필요한 이전 대화 이력을 선택한다.
    2. 현재 질문이 기존 대화 주제를 이어가는지 또는 새로운 주제인지 판단한다.
    
    # 멀티턴 대화 선택 기준
    
    다음 중 하나라도 해당하면 해당 대화 이력을 선택한다.
    
    1. 현재 질문이 이전 대화의 주제나 문제 상황을 이어서 묻는 경우
    2. 현재 질문이 이전 답변을 전제로 하는 후속 질문인 경우
    3. 현재 질문에 등장하는 대상, 개념, 코드, 설정, 문제 상황이 이전 대화에서 정의되었거나 설명된 경우
    4. 현재 질문이 이전 답변의 수정, 확장, 추가 설명을 요청하는 경우
    5. 현재 질문이 이전 대화에서 설명된 내용을 참조해야만 정확히 이해 가능한 경우
    
    # 선택하지 않는 경우
    
    다음 경우에는 선택하지 않는다.
    
    1. 이전 대화와 주제가 다른 경우
    2. 이전 대화를 참고하지 않아도 현재 질문을 독립적으로 이해할 수 있는 경우
    3. 단순 키워드만 겹치고 의미적으로 연결되지 않은 경우
    
    # 토픽 변경 판단 기준
    
    다음 기준을 종합적으로 고려하여 판단한다.
    
    현재 질문이
    
    - 기존 대화의 동일한 문제 해결 과정의 일부인지
    - 동일한 기술, 설정, 오류, 작업 맥락을 이어가는지
    - 이전 답변을 기반으로 추가 질문을 하는지
    
    위 조건에 해당하면 기존 토픽을 유지한다.
    
    반대로,
    
    - 질문의 목적이나 문제 상황이 명확히 바뀐 경우
    - 기술 주제 또는 작업 대상이 바뀐 경우
    - 이전 대화를 참고하지 않아도 완전히 독립적으로 이해되는 질문인 경우
    - 현재 저장된 토픽이 현재 주제 또는 대상이 아니라고 판단되는 경우
    
    새로운 토픽으로 판단한다.
    
    # 중요 규칙
    
    - 키워드 일치 여부만으로 판단하지 않는다
    - 반드시 의미적 연관성(semantic relevance)을 기반으로 판단한다
    - 관련성이 낮으면 선택하지 않는다
    
    # 출력 규칙
    
    반드시 JSON 형식으로만 출력한다.
    
    다른 설명이나 텍스트는 절대 출력하지 않는다.
    
    형식:
    
    {
      "isChangeTopic": boolean,
      "conversationIds": [ID 목록]
    }
    
    # 출력 예시
    
    {
      "isChangeTopic": false,
      "conversationIds": [12, 15]
    }
    
    또는
    
    {
      "isChangeTopic": true,
      "conversationIds": []
    }
    """;
}
