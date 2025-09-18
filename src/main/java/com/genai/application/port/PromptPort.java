package com.genai.application.port;

import com.genai.application.domain.Prompt;

public interface PromptPort {

    /**
     * 프롬 프트 조회
     * @param promptCode 프롬 프트 코드
     * @return 프롬 프트
     */
    Prompt getPrompt(String promptCode);
}
