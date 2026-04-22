package com.genai.core.repository;

import com.genai.core.repository.entity.ChatEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;


public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    Page<ChatEntity> findBySysCreateUserAndMenuCodeOrderBySysCreateDtDesc(String sysCreateUser, String menuCode, Pageable pageable);
}
