package com.genai.core.service.module;

import com.genai.core.service.module.vo.CommonCodeVO;

import java.util.List;

public interface CommonCodeModuleService {

    /**
     * 공통 코드 조회
     *
     * @param code 공통 코드
     * @return 공통 코드
     */
    CommonCodeVO getCommonCode(String code);

    /**
     * 공통 코드 목록 조회
     *
     * @param codeGroup 그룹 코드
     * @return 공통 코드 목록
     */
    List<CommonCodeVO> getCommonCodes(String codeGroup);
}
