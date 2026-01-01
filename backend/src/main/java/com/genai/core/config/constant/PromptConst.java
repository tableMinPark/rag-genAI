package com.genai.core.config.constant;

public class PromptConst {

    // 템플릿 생성 프롬프트 ID
    public static final long GENERATE_MYAI_PROMPT_ID      = 1;   // 나만의 AI 템플릿 생성 프롬프트 ID
    public static final long GENERATE_REPORT_PROMPT_ID    = 2;   // 보고서    템플릿 생성 프롬프트 ID

    // 질의 프롬프트 ID
    public static final long QUESTION_PROMPT_ID           = 3;   // LLM        질의 프롬프트 ID
    public static final long QUESTION_AI_PROMPT_ID        = 4;   // AI         질의 프롬프트 ID
    public static final long QUESTION_MYAI_PROMPT_ID      = 5;   // 나만의 AI  질의 프롬프트 ID

    // 시스템 처리 프롬프트 ID
    public static final long REPORT_PROMPT_ID             = 6;   // 보고서 생성 프롬프트 ID
    public static final long SUMMARY_PROMPT_ID            = 7;   // 요약 프롬프트 ID
    public static final long TRANSLATE_PROMPT_ID          = 8;   // 번역 프롬프트 ID
}
