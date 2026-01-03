package com.genai.core.service.module;

import com.genai.core.service.module.vo.ComnCodeVO;

import java.util.List;

public interface ComnCodeModuleService {

    /**
     * 공통 코드 조회
     *
     * @param code 공통 코드
     * @return 공통 코드
     */
    ComnCodeVO getComnCode(String code);

    /**
     * 공통 코드 목록 조회
     *
     * @param codeGroup 그룹 코드
     * @return 공통 코드 목록
     */
    List<ComnCodeVO> getComnCodes(String codeGroup);
}
