package com.genai.core.repository;

import com.genai.core.repository.entity.PromptEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PromptRepository extends JpaRepository<PromptEntity, Long> {

    Optional<PromptEntity> findByPromptId(Long promptId);
}
