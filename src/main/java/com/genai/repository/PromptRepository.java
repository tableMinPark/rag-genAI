package com.genai.repository;

import com.genai.service.domain.Prompt;

public interface PromptRepository {

    /**
     * 프롬 프트 조회
     * @param promptCode 프롬 프트 코드
     * @return 프롬 프트
     */
    Prompt getPrompt(String promptCode);
}
