package com.genai.core.repository;

import com.genai.core.repository.entity.ChatDetailEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ChatDetailRepository extends JpaRepository<ChatDetailEntity, Long> {

    Long countByChatId(Long chatId);

    Optional<ChatDetailEntity> findTopByChatId(Long chatId);

    List<ChatDetailEntity> findByChatIdOrderBySysCreateDtDesc(Long chatId, Pageable pageable);

}
