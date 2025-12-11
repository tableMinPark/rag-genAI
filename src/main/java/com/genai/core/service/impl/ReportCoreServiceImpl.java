package com.genai.core.service.impl;

import com.genai.core.service.ReportCoreService;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class ReportCoreServiceImpl implements ReportCoreService {

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param file          문서 파일
     * @param userId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Override
    public String generateReport(String reportTitle, String promptContext, MultipartFile file, String userId, long chatId) {
        return "";
    }

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param context       참고 문서
     * @param userId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Override
    public String generateReport(String reportTitle, String promptContext, String context, String userId, long chatId) {
        return "";
    }
}
