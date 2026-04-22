package com.genai.app.chat.service.impl;

import com.genai.app.chat.service.ChatService;
import com.genai.app.chat.service.vo.ChatDetailVO;
import com.genai.app.chat.service.vo.ChatPassageVO;
import com.genai.app.chat.service.vo.ChatVO;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.exception.NotFoundException;
import com.genai.global.enums.Menu;
import com.genai.global.wrapper.PageWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
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
     * 대화 조회 또는 생성
     * chatId가 있으면 기존 대화를 조회하고, 없으면 새 대화를 생성한다.
     *
     * @param userId 사용자 식별자
     * @param title  대화 제목
     * @param menu   메뉴
     * @param chatId 기존 대화 ID (null이면 신규 생성)
     * @return 대화 VO
     */
    @Transactional
    @Override
    public ChatVO getChat(String userId, String title, Menu menu, Long chatId) {

        ChatEntity chatEntity = (chatId != null)
                ? chatRepository.findById(chatId)
                        .orElseThrow(() -> new NotFoundException("대화"))
                : chatRepository.save(ChatEntity.builder()
                        .title(title)
                        .menuCode(menu.name())
                        .sysCreateUser(userId)
                        .build());

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
     * 대화 목록 조회
     *
     * @param userId   사용자 식별자
     * @param menuCode 메뉴 코드
     * @param page     페이지 번호
     * @param size     페이지 크기
     * @return 대화 목록 (페이지)
     */
    @Transactional(readOnly = true)
    @Override
    public PageWrapper<ChatVO> getChats(String userId, String menuCode, int page, int size) {

        Pageable pageable = PageRequest.of(page, size);

        Page<ChatEntity> chatPage = chatRepository.findBySysCreateUserAndMenuCodeOrderBySysCreateDtDesc(userId, menuCode, pageable);

        List<ChatVO> chatVOs = chatPage.getContent().stream()
                .map(chatEntity -> ChatVO.builder()
                        .chatId(chatEntity.getChatId())
                        .title(chatEntity.getTitle())
                        .menuCode(chatEntity.getMenuCode())
                        .sysCreateUser(chatEntity.getSysCreateUser())
                        .sysCreateDt(chatEntity.getSysCreateDt())
                        .sysModifyDt(chatEntity.getSysModifyDt())
                        .build())
                .toList();

        return PageWrapper.<ChatVO>builder()
                .content(chatVOs)
                .isLast(chatPage.isLast())
                .pageNo(chatPage.getNumber())
                .pageSize(chatPage.getSize())
                .totalCount(chatPage.getTotalElements())
                .totalPages(chatPage.getTotalPages())
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
