package com.genai.core.repository;

import com.genai.core.repository.entity.ChatDetailEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatDetailRepository extends JpaRepository<ChatDetailEntity, Long> {
}
