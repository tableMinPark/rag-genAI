package com.genai.core.repository;

import com.genai.core.repository.entity.ChatEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ChatRepository extends JpaRepository<ChatEntity, Long> {

    Optional<ChatEntity> findBySysCreateUser(String sysCreateUser);

    Page<ChatEntity> findBySysCreateUserAndMenuCodeOrderBySysCreateDtDesc(String sysCreateUser, String menuCode, Pageable pageable);
}
