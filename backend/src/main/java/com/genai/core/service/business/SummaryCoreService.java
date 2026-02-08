package com.genai.core.service.business;

import com.genai.core.service.business.vo.SummaryVO;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface SummaryCoreService {

    /**
     * 파일 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param file        문서 파일
     * @param sessionId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    SummaryVO summary(float lengthRatio, MultipartFile file, String sessionId, long chatId);

    /**
     * 텍스트 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param content     사용자 입력 텍스트
     * @param sessionId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    SummaryVO summary(float lengthRatio, String content, String sessionId, long chatId);

    /**
     * 텍스트 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param contents    사용자 입력 텍스트 목록
     * @param sessionId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    SummaryVO summary(float lengthRatio, List<String> contents, String sessionId, long chatId);
}
