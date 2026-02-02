package com.genai.core.service.module.impl;

import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatPassageRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.ChatPassageEntity;
import com.genai.core.service.module.ChatHistoryModuleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatHistoryModuleServiceImpl implements ChatHistoryModuleService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ChatPassageRepository chatPassageRepository;

    /**
     * 대화 이력 저장
     *
     * @param chatId 대화 ID
     * @param chatDetailId 대화 상세 ID
     * @param answer 답변
     * @param chatPassageEntities 참고 문서 목록
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateChatDetail(Long chatId, Long chatDetailId, String rewriteQuery, String answer, List<ChatPassageEntity> chatPassageEntities) {
        // 1. 답변 내용 업데이트
        ChatDetailEntity chatDetailEntity = chatDetailRepository.findById(chatDetailId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        chatDetailEntity.setRewriteQuery(rewriteQuery);
        chatDetailEntity.setAnswer(answer);
        chatDetailRepository.save(chatDetailEntity);

        // 2. 참고 문서 저장
        if (!chatPassageEntities.isEmpty()) {
            chatPassageRepository.saveAll(chatPassageEntities);
        }
    }

    /**
     * 대화 상태 업데이트
     *
     * @param chatId 대화 ID
     * @param chatState 대화 상태
     */
    @Override
    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateChatState(Long chatId, String chatState) {

        // 3. 상태 업데이트 (필요 시)
        if (!chatState.isBlank()) {
            ChatEntity chatEntity = chatRepository.findById(chatId)
                    .orElseThrow(() -> new NotFoundException("대화"));

            chatEntity.setState(chatState);
            chatRepository.save(chatEntity);
        }
    }
}
