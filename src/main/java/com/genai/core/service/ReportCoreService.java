package com.genai.core.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * 보고서 생성 서비스
 */
public interface ReportCoreService {

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param file          문서 파일
     * @param sessionId     세션 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    String generateReport(String reportTitle, String promptContext, MultipartFile file, String sessionId, long chatId);

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param context       참고 문서
     * @param sessionId     세션 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    String generateReport(String reportTitle, String promptContext, String context, String sessionId, long chatId);

}
