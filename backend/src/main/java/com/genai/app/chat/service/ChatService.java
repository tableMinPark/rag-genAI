package com.genai.app.chat.service;

import com.genai.app.chat.service.vo.ChatDetailVO;
import com.genai.app.chat.service.vo.ChatVO;
import com.genai.global.enums.Menu;
import com.genai.global.wrapper.PageWrapper;

import java.util.List;

public interface ChatService {

    /**
     * 대화 조회
     *
     * @param userId 사용자 식별자
     * @param title  대화 제목
     * @param menu   메뉴
     * @return 대화 VO
     */
    ChatVO getChat(String userId, String title, Menu menu);

    /**
     * 대화 목록 조회
     *
     * @param userId   사용자 식별자
     * @param menuCode 메뉴 코드
     * @param page     페이지 번호
     * @param size     페이지 크기
     * @return 대화 목록 (페이지)
     */
    PageWrapper<ChatVO> getChats(String userId, String menuCode, int page, int size);

    /**
     * 대화 상세 목록조회
     *
     * @param userId 사용자 식별자
     * @param chatId 대화 ID
     * @return 대화 VO
     */
    List<ChatDetailVO> getChatDetails(String userId, Long chatId, int page, int size);
}
