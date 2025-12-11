package com.genai.core.config.constant;

public class EmbedConst {

    public static final String MYAI_SOURCE_TYPE           = "SOURCE-TYPE-FILE";
    private static final String MYAI_CATEGORY_CODE_PREFIX = "TRAIN-MYAI-";

    /**
     * 나만의 AI 카테고리 코드 생성
     *
     * @param projectId 프로젝트 ID
     * @return 카테고리 코드
     */
    public static String MYAI_CATEGORY_CODE(long projectId) {
        return MYAI_CATEGORY_CODE_PREFIX + projectId;
    }
}
