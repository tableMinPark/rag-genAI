package com.genai.app.chat.service;

import com.genai.app.chat.service.vo.ChatVO;
import com.genai.global.enums.Menu;

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
}
