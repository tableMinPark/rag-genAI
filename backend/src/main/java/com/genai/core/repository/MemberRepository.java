package com.genai.core.repository;

import com.genai.core.repository.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, String> {
    Optional<MemberEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
