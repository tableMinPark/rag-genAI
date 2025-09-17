package com.genai.repository;

import com.genai.entity.PromptEntity;

import java.util.Optional;

public interface PromptRepository {

    /**
     * 프롬 프트 코드 기준 프롬 프트 엔티티 조회
     *
     * @param promptCode 프롬 프트 코드
     * @return 프롬 프트 엔티티
     */
    Optional<PromptEntity> findByPromptCode(String promptCode);
}
