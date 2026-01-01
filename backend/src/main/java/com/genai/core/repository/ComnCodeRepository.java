package com.genai.core.repository;

import com.genai.core.repository.entity.ComnCodeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ComnCodeRepository extends JpaRepository<ComnCodeEntity, Long> {

    /**
     * 코드 기준 공통 코드 조회
     *
     * @param code 코드
     * @return 공통 코드
     */
    Optional<ComnCodeEntity> findByCode(String code);

    /**
     * 그룹 코드 기준 공통 코드 목록 조회
     *
     * @param codeGroup 그룹 코드
     * @return 공통 코드 목록
     */
    List<ComnCodeEntity> findComnCodeByCodeGroupOrderBySortOrder(String codeGroup);
}
