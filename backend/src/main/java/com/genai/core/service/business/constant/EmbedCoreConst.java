package com.genai.core.service.business.constant;

public class EmbedCoreConst {

    /*
     * ##############################################
     * 색인 속성
     * ##############################################
     */
    public static final int RETRY_COUNT = 3;
    public static final int RETRY_DELAY = 1000 * 10;

    /*
     * ##############################################
     * 임베딩 속성
     * ##############################################
     */
    public static final long   EMBED_VERSION      = 1L;
    public static final String EMBED_UPDATE_STATE = "UPDATE_STATE_INSERT";
    public static final String EMBED_SOURCE_TYPE  = "EMBED_LIVE";
    public static final String EMBED_SELECT_TYPE  = "SELECT_TYPE_TOKEN";
}
