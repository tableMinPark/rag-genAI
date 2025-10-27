package com.genai.service;

import com.genai.global.enums.MenuType;
import com.genai.service.vo.QuestionVo;

public interface ChatService {

    /**
     * 질의
     *
     * @param menuType  컬렉션 타입
     * @param query     질의문
     * @param sessionId 세션 식별자
     */
    QuestionVo questionRagUseCase(String query, String sessionId, MenuType menuType);

    /**
     * LLM 질의
     *
     * @param query         질의문
     * @param context       참고 문서
     * @param promptContext 시스템 프롬 프트
     * @param sessionId     세션 식별자
     * @param maxToken      최대 토큰 수
     * @param temperature   창의성 조정 값
     * @param topP          응답 가변성 조정 값
     */
    QuestionVo questionUseCase(String query, String sessionId, String context, String promptContext, int maxToken, double temperature, double topP);
}