package com.genai.core.service.business.impl;

import com.genai.common.utils.*;
import com.genai.common.vo.UploadFileVO;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.CommonCodeRepository;
import com.genai.core.repository.DictionaryRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.CommonCodeEntity;
import com.genai.core.service.business.TranslateCoreService;
import com.genai.core.service.business.constant.TranslateCoreConst;
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.DictionaryVO;
import com.genai.core.service.business.vo.PrepareVO;
import com.genai.core.service.business.vo.TranslateVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.TranslateModuleService;
import com.genai.core.service.module.vo.PartTranslateContextVO;
import com.genai.core.service.module.vo.PartTranslateState;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;

import java.time.Duration;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateCoreServiceImpl implements TranslateCoreService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final DictionaryRepository dictionaryRepository;
    private final TranslateModuleService translateModuleService;
    private final ChatHistoryModuleService chatHistoryModuleService;
    private final CommonCodeRepository commonCodeRepository;

    /**
     * 파일 번역
     *
     * @param afterLang  번역 언어 코드
     * @param file       문서 파일
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @param containDic 사전 포함 여부
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String afterLang, MultipartFile file, String sessionId, long chatId, boolean containDic) {

        UploadFileVO uploadFile = FileUtil.uploadFileTemp(file);

        String extractContent = ExtractUtil.extractText(uploadFile.getUrl(), uploadFile.getExt());
        String content = HtmlUtil.removeHtmlExceptTable(extractContent);

        List<String> contents = StringUtil.tokenize(content, TranslateCoreConst.CHUNK_PART_TOKEN_SIZE);

        return this.translate(afterLang, contents, sessionId, chatId, containDic);
    }

    /**
     * 텍스트 번역
     *
     * @param afterLang 번역 언어 코드
     * @param content   사용자 입력 텍스트
     * @param sessionId 세션 ID
     * @param chatId    대화 정보 ID
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String afterLang, String content, String sessionId, long chatId, boolean containDic) {

        content = HtmlUtil.removeHtmlExceptTable(content);
        List<String> contents = StringUtil.tokenize(content, TranslateCoreConst.CHUNK_PART_TOKEN_SIZE);

        return this.translate(afterLang, contents, sessionId, chatId, containDic);
    }

    /**
     * 텍스트 번역
     *
     * @param afterLang 번역 언어 코드
     * @param contents  사용자 입력 텍스트 목록
     * @param sessionId 세션 ID
     * @param chatId    대화 정보 ID
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String afterLang, List<String> contents, String sessionId, long chatId, boolean containDic) {

        CommonCodeEntity afterLangCode = commonCodeRepository.findByCode(afterLang)
                .orElseThrow(() -> new NotFoundException("번역 언어"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        String query = String.format("""
                # 번역 요청 정보
                - 번역 언어: %s
                - 사전 사용 여부: %s
                """, afterLangCode.getCodeName(), containDic);

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        // 사전 목록
        List<DictionaryVO> dictionaries = containDic
                ? dictionaryRepository.findByLanguageCode(afterLangCode.getCode()).stream()
                .map(dictionaryEntity -> DictionaryVO.builder()
                        .dictionary(dictionaryEntity.getDictionary())
                        .dictionaryDesc(dictionaryEntity.getDictionaryDesc())
                        .build())
                .toList()
                : Collections.emptyList();

        // 사전 색인 생성
        AhoCorasick ahoCorasick = AhoCorasick.init(dictionaries.stream()
                .map(dictionaryEntity -> dictionaryEntity.getDictionary().toLowerCase())
                .toList());

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();
        String translateId = StringUtil.generateRandomId();

        Flux<PartTranslateContextVO> partTranslateFlux = translateModuleService
                .partTranslate(PartTranslateState.init(
                                StringUtil.indexingContent(contents),
                                afterLangCode.getCodeName(),
                                dictionaries,
                                containDic
                        ), ahoCorasick
                )
                .cache();

        Flux<StreamEvent> translateFlux = partTranslateFlux
                .sort(Comparator.comparingInt(PartTranslateContextVO::getIndex))
                .doOnNext(partTranslateContext -> answerAccumulator.append(partTranslateContext.getPartTranslate()))
                .map(partTranslateContext ->StreamEvent.answer(translateId, partTranslateContext.getPartTranslate()))
                .delayElements(Duration.ofMillis(100));

        Flux<StreamEvent> partTranslateProgressFlux = Flux.concat(
                Flux.just(StreamEvent.prepare(StringUtil.generateRandomId(), PrepareVO.builder()
                        .progress(0)
                        .message("문서 전처리중")
                        .build())),
                partTranslateFlux.map(PartTranslateContextVO::getStreamEvent)
        );

        Flux<StreamEvent> answerStream = Flux.concat(partTranslateProgressFlux, translateFlux)
                .doOnCancel(() -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .doOnError(throwable -> chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId()))
                .onErrorMap(throwable -> new RuntimeException("스트림 처리 중 예외 발생", throwable))
                .doOnComplete(() -> {
                    // 대화 이력 업데이트
                    chatHistoryModuleService.updateChatDetail(
                            chatId,
                            chatDetailEntity.getMsgId(),
                            "",
                            answerAccumulator.toString().trim(),
                            Collections.emptyList()
                    );
                });

        return TranslateVO.builder()
                .answerStream(answerStream)
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
