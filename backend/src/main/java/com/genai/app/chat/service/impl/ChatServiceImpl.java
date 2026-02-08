package com.genai.app.chat.service.impl;

import com.genai.app.chat.service.ChatService;
import com.genai.app.chat.service.vo.ChatVO;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.global.enums.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;

    /**
     * 대화 조회
     *
     * @param userId 사용자 식별자
     * @param title  대화 제목
     * @param menu   메뉴
     * @return 대화 VO
     */
    @Transactional
    @Override
    public ChatVO getChat(String userId, String title, Menu menu) {

        ChatEntity chatEntity = chatRepository.findBySysCreateUser(userId)
                .orElseGet(() -> chatRepository.save(ChatEntity.builder()
                        .title(title)
                        .menuCode(menu.name())
                        .sysCreateUser(userId)
                        .build()));

        return ChatVO.builder()
                .chatId(chatEntity.getChatId())
                .state(chatEntity.getState())
                .title(chatEntity.getTitle())
                .menuCode(chatEntity.getMenuCode())
                .sysCreateUser(chatEntity.getSysCreateUser())
                .sysCreateDt(chatEntity.getSysCreateDt())
                .sysModifyDt(chatEntity.getSysModifyDt())
                .build();
    }
}
