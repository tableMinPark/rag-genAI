package com.genai.core.service.module.impl;

import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.CommonCodeRepository;
import com.genai.core.repository.entity.CommonCodeEntity;
import com.genai.core.service.module.CommonCodeModuleService;
import com.genai.core.service.module.vo.CommonCodeVO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonCodeModuleServiceImpl implements CommonCodeModuleService {

    private final CommonCodeRepository commonCodeRepository;

    /**
     * 공통 코드 조회
     *
     * @param code 공통 코드
     * @return 공통 코드
     */
    @Transactional(readOnly = true)
    @Override
    public CommonCodeVO getCommonCode(String code) {

        CommonCodeEntity commonCodeEntity = commonCodeRepository.findByCode(code)
                .orElseThrow(() -> new NotFoundException("공통 코드"));

        return CommonCodeVO.builder()
                .code(commonCodeEntity.getCode())
                .codeName(commonCodeEntity.getCodeName())
                .codeGroup(commonCodeEntity.getCodeGroup())
                .sortOrder(commonCodeEntity.getSortOrder())
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
    public List<CommonCodeVO> getCommonCodes(String codeGroup) {

        List<CommonCodeEntity> comnCodeEntities = commonCodeRepository.findComnCodeByCodeGroupOrderBySortOrder(codeGroup);

        return comnCodeEntities.stream()
                .map(commonCodeEntity -> CommonCodeVO.builder()
                        .code(commonCodeEntity.getCode())
                        .codeName(commonCodeEntity.getCodeName())
                        .codeGroup(commonCodeEntity.getCodeGroup())
                        .sortOrder(commonCodeEntity.getSortOrder())
                        .build())
                .toList();
    }
}
