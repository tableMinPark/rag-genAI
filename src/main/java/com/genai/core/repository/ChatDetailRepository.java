package com.genai.core.repository;

import com.genai.core.repository.entity.ChatDetailEntity;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ChatDetailRepository extends JpaRepository<ChatDetailEntity, Long> {

    List<ChatDetailEntity> findByChatIdOrderBySysCreateDtDesc(Long chatId, Pageable pageable);

}
