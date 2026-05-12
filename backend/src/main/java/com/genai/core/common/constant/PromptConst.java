package com.genai.core.common.constant;

public class PromptConst {

    public static final String PROMPT_ROLE_CODE_GROUP     = "PROMPT_ROLE";
    public static final String PROMPT_TONE_CODE_GROUP     = "PROMPT_TONE";
    public static final String PROMPT_STYLE_CODE_GROUP    = "PROMPT_STYLE";

    // 템플릿 생성 프롬프트 ID
    public static final long GENERATE_MYAI_PROMPT_ID      = 1;   // 나만의 AI 템플릿 생성 프롬프트 ID
    public static final long GENERATE_REPORT_PROMPT_ID    = 2;   // 보고서    템플릿 생성 프롬프트 ID

    // 질의 프롬프트 ID
    public static final long QUESTION_PROMPT_ID           = 3;   // LLM 질의 프롬프트 ID
    public static final long QUESTION_AI_PROMPT_ID        = 4;   // AI  질의 프롬프트 ID

    // 시스템 처리 프롬프트 ID
    public static final long SUMMARY_PROMPT_ID            = 5;   // 요약 프롬프트 ID
    public static final long TRANSLATE_PROMPT_ID          = 6;   // 번역 프롬프트 ID
}
