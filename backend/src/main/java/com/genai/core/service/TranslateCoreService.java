package com.genai.core.service;

import com.genai.core.service.vo.TranslateVO;
import org.springframework.web.multipart.MultipartFile;

/**
 * 번역 서비스
 */
public interface TranslateCoreService {

    /**
     * 파일 번역
     *
     * @param beforeLang 원문 언어 코드
     * @param afterLang  번역 언어 코드
     * @param file       문서 파일
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @param containDic 사전 포함 여부
     * @return 번역 결과 문자열
     */
    TranslateVO translate(String beforeLang, String afterLang, MultipartFile file, String sessionId, long chatId, boolean containDic);

    /**
     * 텍스트 번역
     *
     * @param beforeLang 원문 언어 코드
     * @param afterLang  번역 언어 코드
     * @param content    사용자 입력 텍스트
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @return 번역 결과 문자열
     */
    TranslateVO translate(String beforeLang, String afterLang, String content, String sessionId, long chatId, boolean containDic);

}
