package com.genai.core.service;

import com.genai.core.service.vo.ReportVO;
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
    ReportVO generateReport(String reportTitle, String promptContext, MultipartFile file, String sessionId, long chatId);

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param content       참고 문서
     * @param sessionId     세션 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    ReportVO generateReport(String reportTitle, String promptContext, String content, String sessionId, long chatId);

}
