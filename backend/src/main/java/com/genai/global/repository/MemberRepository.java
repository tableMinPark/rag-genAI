package com.genai.global.repository;

import com.genai.global.repository.entity.MemberEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MemberRepository extends JpaRepository<MemberEntity, String> {
    Optional<MemberEntity> findByUserId(String userId);
    boolean existsByUserId(String userId);
}
