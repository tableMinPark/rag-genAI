package com.genai.core.service.business;

/**
 * 프롬프트 서비스
 */
public interface PromptCoreService {

    /**
     * 나만의 AI 프롬프트 생성
     *
     * @param role        전문 분야 및 역할
     * @param answerTone  답변 말투
     * @param answerStyle 답변 스타일
     * @return 프롬프트 VO
     */
    String generateMyAiPrompt(String role, String answerTone, String answerStyle);

    /**
     * 보고서 프롬프트 생성
     *
     * @param content 사용자 입력 텍스트
     * @return 프롬프트 VO
     */
    String generateReportPrompt(String content);

    /**
     * 프롬프트 생성
     *
     * @param promptId 생성 프롬프트 ID
     * @param content  프롬프트 생성 컨텍스트
     * @return 프롬프트 VO
     */
    String generatePrompt(long promptId, String content);

}