package com.genai.application.usecase;

import com.genai.application.enums.CollectionType;
import com.genai.application.vo.QuestionVo;

public interface ChatUseCase {

    /**
     * 질의
     *
     * @param collectionType 컬렉션 타입
     * @param promptCode     시스템 프롬 프트 코드
     * @param query          질의문
     * @param sessionId      세션 식별자
     */
    QuestionVo questionRagUseCase(CollectionType collectionType, String promptCode, String query, String sessionId);

    /**
     * LLM 질의
     *
     * @param promptCode    시스템 프롬 프트 코드
     * @param query         질의문
     * @param context       참고 문서
     * @param promptContext 시스템 프롬 프트
     * @param sessionId     세션 식별자
     * @param maxToken      최대 토큰 수
     * @param temperature   창의성 조정 값
     * @param topP          응답 가변성 조정 값
     */
    QuestionVo questionUseCase(String promptCode, String query, String context, String promptContext, String sessionId, int maxToken, double temperature, double topP);
}
