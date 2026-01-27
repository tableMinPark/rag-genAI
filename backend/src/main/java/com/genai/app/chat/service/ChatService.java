package com.genai.app.chat.service;

import com.genai.app.chat.service.vo.ChatVO;

public interface ChatService {


    /**
     * 대화 조회
     *
     * @param userId 사용자 식별자
     * @param title 대화 제목
     * @param menuCode 메뉴 코드
     * @return 대화 VO
     */
    ChatVO getChat(String userId, String title, String menuCode);
}
