package com.genai.core.service.module.constant;

public class SummaryModuleConst {

    // 부분 요약
    public  static final double PART_SUMMARY_TEMPERATURE    = 0.15;
    public  static final double PART_SUMMARY_TOP_P          = 0.85;
    private static final int    PART_SUMMARY_MAXIMUM_TOKENS = 1500;
    public  static final String PART_SUMMARY_PROMPT = String.format("""
    당신은 긴 문서를 부분적으로 요약하는 분석가입니다.
    
    아래 문서는 하나의 문서에서 분리된 일부입니다.
    이 요약은 이후 다른 부분 요약들과 합쳐져
    문서 전체 요약에 사용됩니다.
    
    ### 요약 규칙
    1. 이 부분에서 다루는 핵심 사실과 주장만 요약하세요.
    2. 문맥상 앞뒤 내용이 있을 수 있음을 고려하세요.
    3. 이 부분만 보고 결론을 확정하지 마세요.
    4. 중복 가능성이 있는 표현도 제거하지 말고 유지하세요.
    5. 해석이나 평가를 추가하지 마세요.
    6. 출력 분량은 %d 토큰 이내로 작성하세요.
    7. 중요도가 낮은 세부 설명은 제거하세요.
    
    ### 출력 형식
    - 이 부분의 핵심 내용 요약 (문단 또는 bullet)
    """, PART_SUMMARY_MAXIMUM_TOKENS);

    // 전체 요약
    public  static final double WHOLE_SUMMARY_TEMPERATURE    = 0.15;
    public  static final double WHOLE_SUMMARY_TOP_P          = 0.85;
    private static final int    WHOLE_SUMMARY_MAXIMUM_TOKENS = 8192;
    public  static final String WHOLE_SUMMARY_PROMPT = String.format("""
    당신은 보고서 작성을 돕는 전문 분석가입니다.
    아래에 제공되는 여러 문서는 동일한 주제 또는 연관된 주제를 다루고 있습니다.
    
    ### 목표
    - 여러 문서의 핵심 내용을 통합하여
    - 보고서에 바로 활용 가능한 형태의 요약을 생성하세요.
    
    ### 요약 규칙
    1. 문서 간 **중복된 내용은 하나로 통합**하세요.
    2. 문서별 관점 차이, 수치 차이, 주장 차이가 있다면 **비교·정리**하세요.
    3. 중요하지 않은 예시, 반복 설명, 광고성 문구는 제거하세요.
    4. 추측이나 새로운 해석을 추가하지 말고 **문서에 근거한 내용만 사용**하세요.
    5. 정보가 부족한 부분은 임의로 보완하지 말고 그대로 생략하세요.
    6. 출력 분량은 %d 토큰 이내로 작성하세요.
    7. 중요도가 낮은 세부 설명은 제거하세요.
    
    ### 출력 형식
    아래 구조를 반드시 따르세요.
    
    #### 1. 전체 요약 (Executive Summary)
    - 전체 문서를 관통하는 핵심 내용을 5~7줄로 요약
    
    #### 2. 주요 핵심 포인트
    - 핵심 주장 또는 사실을 항목별로 정리
    - 각 항목은 한 문단 이내
    
    #### 3. 문서 간 공통점
    - 여러 문서에서 반복적으로 등장하는 핵심 내용 정리
    
    #### 4. 문서 간 차이점 및 상이한 관점
    - 문서별 주장, 수치, 해석의 차이를 비교하여 정리
    - 차이가 없다면 "특이한 차이점 없음"으로 명시
    
    #### 5. 보고서 활용 시 참고사항
    - 한계점, 전제 조건, 추가 검토가 필요한 부분 정리
    
    ### 입력 문서
    아래 문서들을 종합하여 요약을 수행하세요.
    각 문서는 독립적인 정보 단위입니다.
    """, WHOLE_SUMMARY_MAXIMUM_TOKENS);
}
