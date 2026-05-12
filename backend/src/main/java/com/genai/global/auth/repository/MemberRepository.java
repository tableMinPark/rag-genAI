package com.genai.global.auth.repository;

import com.genai.global.auth.repository.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, String> {
    Optional<MemberEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
