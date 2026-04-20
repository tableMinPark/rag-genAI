package com.genai.app.chat.service.impl;

import com.genai.app.chat.service.ChatService;
import com.genai.app.chat.service.vo.ChatDetailVO;
import com.genai.app.chat.service.vo.ChatPassageVO;
import com.genai.app.chat.service.vo.ChatVO;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.global.enums.Menu;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatServiceImpl implements ChatService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;

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

    /**
     * 대화 상세 목록조회
     *
     * @param userId 사용자 식별자
     * @param chatId 대화 ID
     * @return 대화 VO
     */
    @Transactional
    @Override
    public List<ChatDetailVO> getChatDetails(String userId, Long chatId, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        List<ChatDetailEntity> chatDetailEntities = chatDetailRepository.findByChatIdAndAnswerIsNotNullOrderBySysCreateDtDesc(chatId, pageable);

        return chatDetailEntities.stream()
                .map(chatDetailEntity -> ChatDetailVO.builder()
                        .msgId(chatDetailEntity.getMsgId())
                        .chatId(chatDetailEntity.getChatId())
                        .query(chatDetailEntity.getRewriteQuery())
                        .answer(chatDetailEntity.getAnswer())
                        .sysCreateDt(chatDetailEntity.getSysCreateDt())
                        .sysModifyDt(chatDetailEntity.getSysModifyDt())
                        .passages(ChatPassageVO.toList(chatDetailEntity.getChatPassages()))
                        .build())
                .toList();
    }
}
