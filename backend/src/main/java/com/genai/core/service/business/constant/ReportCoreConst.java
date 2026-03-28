package com.genai.core.service.business.constant;

public class ReportCoreConst {

    public static final int CHUNK_PART_TOKEN_SIZE   = 4500;
    public static final int CHUNK_MAX_TOKEN_SIZE    = 58000;

    /*
     * #######################################
     * 보고거 생성 프롬프트
     * #######################################
     */
    public  static final double REPORT_TEMPERATURE = 0.15;
    public  static final double REPORT_TOP_P       = 0.85;
    private static final String REPORT_TITLE_REFIX = "{{ report_title }}";
    private static final String REPORT_GENERATE_INFO_PROMPT = """
    # 보고서 생성 참고 정보
    - 보고서 제목: {{ report_title }}
    """;
    public static String REPORT_GENERATE_INFO_PROMPT(String reportTitle) {
        return REPORT_GENERATE_INFO_PROMPT.replace(REPORT_TITLE_REFIX, reportTitle);
    }
}
