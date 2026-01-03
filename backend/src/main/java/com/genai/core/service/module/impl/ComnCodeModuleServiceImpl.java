package com.genai.core.service.module.impl;

import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ComnCodeRepository;
import com.genai.core.repository.entity.ComnCodeEntity;
import com.genai.core.service.module.ComnCodeModuleService;
import com.genai.core.service.module.vo.ComnCodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ComnCodeModuleServiceImpl implements ComnCodeModuleService {

    private final ComnCodeRepository comnCodeRepository;

    /**
     * 공통 코드 조회
     *
     * @param code 공통 코드
     * @return 공통 코드
     */
    @Transactional(readOnly = true)
    @Override
    public ComnCodeVO getComnCode(String code) {

        ComnCodeEntity comnCodeEntity = comnCodeRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("공통 코드"));

        return ComnCodeVO.builder()
                .codeId(comnCodeEntity.getCodeId())
                .code(comnCodeEntity.getCode())
                .codeName(comnCodeEntity.getCodeName())
                .codeGroup(comnCodeEntity.getCodeGroup())
                .sortOrder(comnCodeEntity.getSortOrder())
                .build();
    }

    /**
     * 공통 코드 목록 조회
     *
     * @param codeGroup 그룹 코드
     * @return 공통 코드 목록
     */
    @Transactional(readOnly = true)
    @Override
    public List<ComnCodeVO> getComnCodes(String codeGroup) {

        List<ComnCodeEntity> comnCodeEntities = comnCodeRepository.findComnCodeByCodeGroupOrderBySortOrder(codeGroup);

        return comnCodeEntities.stream()
                .map(comnCodeEntity -> ComnCodeVO.builder()
                        .codeId(comnCodeEntity.getCodeId())
                        .code(comnCodeEntity.getCode())
                        .codeName(comnCodeEntity.getCodeName())
                        .codeGroup(comnCodeEntity.getCodeGroup())
                        .sortOrder(comnCodeEntity.getSortOrder())
                        .build())
                .toList();
    }
}
