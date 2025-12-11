package com.genai.core.repository;

import com.genai.core.repository.entity.ChatPassageEntity;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChatPassageRepository extends JpaRepository<ChatPassageEntity, Long> {
}
