package com.genai.core.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.core.config.constant.PromptConst;
import com.genai.core.config.constant.QuestionConst;
import com.genai.core.config.constant.SearchConst;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.repository.wrapper.Rerank;
import com.genai.core.repository.wrapper.Search;
import com.genai.core.service.QuestionCoreService;
import com.genai.core.service.vo.AnswerVO;
import com.genai.core.service.vo.ContextVO;
import com.genai.core.service.vo.DocumentVo;
import com.genai.core.service.vo.QuestionVO;
import com.genai.core.type.CollectionType;
import com.genai.core.type.CollectionTypeFactory;
import com.genai.exception.NotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class QuestionCoreServiceImpl implements QuestionCoreService {

    private final SearchRepository searchRepository;
    private final ModelRepository modelRepository;
    private final PromptRepository promptRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ChatPassageRepository chatPassageRepository;
    private final CollectionTypeFactory collectionTypeFactory;
    private final ObjectMapper objectMapper;

    /**
     * 질문 & 답변
     *
     * @param query     질의문
     * @param sessionId 사용자 ID
     * @param chatId    대화 ID
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO question(String query, String sessionId, long chatId) {
        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = promptRepository.findByPromptId(PromptConst.QUESTION_AI_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 질의 이력 생성
        chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .speaker(sessionId)
                .content(query)
                .build());

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .speaker(QuestionConst.CHAT_HISTORY_SYSTEM_NAME)
                .content("")
                .build());

        // LLM 답변 스트림 요청
        Flux<List<AnswerVO>> answerStream = modelRepository
                .generateStreamAnswer(query, sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getConvertContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.isInference())
                                .build())
                        .toList());

        return QuestionVO.builder()
                .answerStream(answerStream)
                .documents(Collections.emptyList())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }

    /**
     * AI 질문 & 답변
     *
     * @param query         질의문
     * @param sessionId     사용자 ID
     * @param chatId        대화 ID
     * @param categoryCodes 카테고리 코드 목록
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionAi(String query, String sessionId, long chatId, List<String> categoryCodes) {
        return this.questionByCollectionId(query, sessionId, chatId, PromptConst.QUESTION_AI_PROMPT_ID, collectionTypeFactory.ai(), categoryCodes);
    }

    /**
     * 나만의 AI 질문 & 답변
     *
     * @param query     질의문
     * @param sessionId 사용자 ID
     * @param chatId    대화 ID
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionMyAi(String query, String sessionId, long chatId, long promptId) {
        return this.questionByCollectionId(query, sessionId, chatId, promptId, collectionTypeFactory.myai(), Collections.emptyList());
    }

    /**
     * 컬렉션 ID 기준 질문 & 답변
     *
     * @param query          질의문
     * @param sessionId      세션 ID
     * @param promptId       프롬프트 ID
     * @param collectionType 컬렉션
     * @param chatId         대화 ID
     * @param categoryCodes  검색 필터
     * @return 답변 VO
     */
    @Transactional
    @Override
    public QuestionVO questionByCollectionId(String query, String sessionId, long chatId, long promptId, CollectionType collectionType, List<String> categoryCodes) {

        // 시스템 프롬 프트 조회
        PromptEntity promptEntity = promptRepository.findByPromptId(promptId)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        // 검색 결과 목록 (key 를 통한 중복 제거)
        Map<String, Search<DocumentEntity>> searchEntityMap = new HashMap<>();

        // 키워드 검색
        List<Search<DocumentEntity>> keywordSearchEntities = searchRepository.keywordSearch(collectionType, query, SearchConst.KEYWORD_TOP_K, sessionId, categoryCodes);
        keywordSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));

        // 벡터 검색
        List<Search<DocumentEntity>> vectorSearchEntities = searchRepository.vectorSearch(collectionType, query, SearchConst.VECTOR_TOP_K, categoryCodes);
        vectorSearchEntities.forEach(searchEntity -> searchEntityMap.put(searchEntity.getFields().getChunkId(), searchEntity));

        // 키워드 검색 결과, 벡터 검색 결과 변환
        List<Rerank> rerankEntities = searchRepository.rerank(query, searchEntityMap.values().stream()
                .map(searchEntity -> Rerank.builder()
                        .document(searchEntity.getFields())
                        .build())
                .toList());

        // 리랭킹
        List<Rerank> topRerankEntities = rerankEntities.stream()
                .filter(rerankEntity -> rerankEntity.getRerankScore() >= SearchConst.RERANK_SCORE_MIN)
                .toList();

        // 상위 RERANK_TOP_K 개 추출
        List<Rerank> finalTopRerankEntities = topRerankEntities
                .subList(0, Math.min(SearchConst.RERANK_TOP_K, topRerankEntities.size()));

        // Context 생성
        List<ContextVO> contextVos = finalTopRerankEntities.stream()
                .map(rerank -> ContextVO.builder()
                        .title(rerank.getDocument().getTitle())
                        .subTitle(rerank.getDocument().getSubTitle())
                        .thirdTitle(rerank.getDocument().getThirdTitle())
                        .content(rerank.getDocument().getCompactContent())
                        .subContent(rerank.getDocument().getSubContent())
                        .build())
                .toList();

        // TODO: 멀티턴 로직 추가

        // Context Json 직렬화
        String contextJson;
        try {
            contextJson = objectMapper.writeValueAsString(contextVos);
        } catch (JsonProcessingException e) {
            // 직렬화 실패 경우
            StringBuilder contextBuilder = new StringBuilder();
            for (Rerank rerank : rerankEntities) {
                contextBuilder.append(rerank.getDocument().getContext());
            }
            contextJson = contextBuilder.toString();
        }

        // LLM 답변 스트림 요청
        Flux<List<AnswerVO>> answerStream = modelRepository
                .generateStreamAnswer(query, contextJson.trim(), sessionId, promptEntity)
                .map(answerEntities -> answerEntities.stream()
                        .map(answerEntity -> AnswerVO.builder()
                                .id(answerEntity.getId())
                                .content(answerEntity.getConvertContent())
                                .finishReason(answerEntity.getFinishReason())
                                .isInference(answerEntity.isInference())
                                .build())
                        .toList());

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 질의 이력 생성
        chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .speaker(sessionId)
                .content(query)
                .build());

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .speaker(QuestionConst.CHAT_HISTORY_SYSTEM_NAME)
                .content("")
                .build());

        StringBuilder answerBuilder = new StringBuilder();
        answerStream.subscribe(answerVos -> {
            for (AnswerVO answerVo : answerVos) {
                if (!answerVo.isInference()) {
                    answerBuilder.append(answerVo.getContent());
                }
            }
        }, throwable -> {
        }, () -> {
            // 답변 병합 (답변만 적재)
            String content = answerBuilder.toString();

            content = content.replace("&nbsp", " ");
            content = content.replace("\\n", "\n");

            // 추론 및 답변 등록
            chatDetailEntity.setContent(content.trim());
            chatDetailRepository.save(chatDetailEntity);

            // 참고 문서 목록
            List<ChatPassageEntity> chatPassageEntities = finalTopRerankEntities.stream()
                    .map(rerankEntity -> {

                        String context = rerankEntity.getDocument().getTitle() + "\n" +
                                rerankEntity.getDocument().getSubTitle() + "\n" +
                                rerankEntity.getDocument().getThirdTitle() + "\n" +
                                rerankEntity.getDocument().getContent() + "\n" +
                                rerankEntity.getDocument().getSubContent() + "\n";

                        context = context.replace("\\n", "\n");

                        return ChatPassageEntity.builder()
                                .msgId(chatDetailEntity.getMsgId())
                                .fileDetailId(rerankEntity.getDocument().getFileDetailId())
                                .sourceType(rerankEntity.getDocument().getSourceType())
                                .categoryCode(rerankEntity.getDocument().getCategoryCode())
                                .content(context)
                                .build();
                    })
                    .toList();

            // 참고 문서 등록
            chatPassageRepository.saveAll(chatPassageEntities);
        });

        return QuestionVO.builder()
                .answerStream(answerStream)
                .documents(finalTopRerankEntities.stream()
                        .map(rerank -> DocumentVo.builder()
                                .title(rerank.getDocument().getTitle())
                                .subTitle(rerank.getDocument().getSubContent())
                                .thirdTitle(rerank.getDocument().getThirdTitle())
                                .content(rerank.getDocument().getContent())
                                .subContent(rerank.getDocument().getSubContent())
                                .originFileName(rerank.getDocument().getOriginFileName())
                                .categoryCode(rerank.getDocument().getCategoryCode())
                                .sourceType(rerank.getDocument().getSourceType())
                                .url(rerank.getDocument().getUrl())
                                .ext(rerank.getDocument().getExt())
                                .build())
                        .toList())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
