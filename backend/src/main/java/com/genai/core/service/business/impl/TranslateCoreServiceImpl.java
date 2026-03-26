package com.genai.core.service.business.impl;

import com.genai.common.utils.*;
import com.genai.common.vo.UploadFileVO;
import com.genai.core.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.service.business.TranslateCoreService;
import com.genai.core.service.business.constant.TranslateCoreConst;
import com.genai.core.service.business.subscriber.StreamEvent;
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
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.time.Duration;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateCoreServiceImpl implements TranslateCoreService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final DictionaryRepository dictionaryRepository;
    private final PromptRepository promptRepository;
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

        List<String> contents = new ArrayList<>();

        UploadFileVO uploadFile = FileUtil.uploadFileTemp(file);

        String extractContent = ExtractUtil.extractText(uploadFile.getUrl(), uploadFile.getExt());
        String content = HtmlUtil.convertTableHtmlToMarkdown(extractContent);

        StringBuilder contentBuilder = new StringBuilder();
        for (String line : content.lines().toList()) {
            if (contentBuilder.length() + line.length() > TranslateCoreConst.CHUNK_PART_TOKEN_SIZE) {
                contents.add(contentBuilder.toString().trim());
                contentBuilder = new StringBuilder();
            }
            contentBuilder.append(line).append("\n");
        }

        if (!contentBuilder.toString().trim().isEmpty()) {
            contents.add(contentBuilder.toString().trim());
        }

        return this.translate(afterLang, contents, sessionId, chatId, containDic);
    }

    /**
     * 텍스트 번역
     *
     * @param afterLang  번역 언어 코드
     * @param content    사용자 입력 텍스트
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String afterLang, String content, String sessionId, long chatId, boolean containDic) {

        List<String> contents = new ArrayList<>();

        StringBuilder contentBuilder = new StringBuilder();
        for (String line : content.lines().toList()) {
            if (contentBuilder.length() + line.length() > TranslateCoreConst.CHUNK_PART_TOKEN_SIZE) {
                contents.add(contentBuilder.toString().trim());
                contentBuilder = new StringBuilder();
            }
            contentBuilder.append(line).append("\n");
        }

        if (!contentBuilder.toString().trim().isEmpty()) {
            contents.add(contentBuilder.toString().trim());
        }

        return this.translate(afterLang, contents, sessionId, chatId, containDic);
    }

    /**
     * 텍스트 번역
     *
     * @param afterLang  번역 언어 코드
     * @param contents   사용자 입력 텍스트 목록
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String afterLang, List<String> contents, String sessionId, long chatId, boolean containDic) {

        CommonCodeEntity afterLangCode = commonCodeRepository.findByCode(afterLang)
                .orElseThrow(() -> new NotFoundException("번역 언어"));

        String translateInfo = String.format("""
        # 번역 처리 정보
        - 번역 대상 언어: %s
        - 사전 사용 여부: %b
        """, afterLangCode.getCodeName(), containDic);

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(translateInfo)
                .build());

        PromptEntity promptEntity = promptRepository.findById(PromptConst.TRANSLATE_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        // 사전 색인 생성
        AhoCorasick ahoCorasick = new AhoCorasick();
        List<DictionaryEntity> dictionaryEntities = new ArrayList<>();
        if (containDic) {
            dictionaryEntities.addAll(dictionaryRepository.findAll());
            dictionaryEntities.forEach(dictionaryEntity -> ahoCorasick.insert(dictionaryEntity.getDictionary().toLowerCase()));
            ahoCorasick.build();
        }

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();
        PartTranslateState partTranslateState = PartTranslateState.init(StringUtil.indexingContent(contents));

        Flux<PartTranslateContextVO> partTranslateFlux = Mono.just(partTranslateState)
                        .flatMapMany(state -> Flux.fromIterable(state.getIndexedContents())
                                .flatMap(indexedContent -> {
                                    int index = indexedContent.getIndex();
                                    String content = indexedContent.getContent();
                                    String dictionaryContent = "";

                                    // 사전 목록 조회
                                    if (containDic) {
                                        StringBuilder dictionaryContentBuilder = new StringBuilder();
                                        Set<String> matchedWords = ahoCorasick.search(content.toLowerCase());
                                        dictionaryEntities.stream()
                                                .filter(dictionaryEntity -> matchedWords.contains(dictionaryEntity.getDictionary().toLowerCase()))
                                                .filter(dictionaryEntity -> afterLang.equals(dictionaryEntity.getLanguage().getCode()))
                                                .forEach(dictionaryEntity -> {
                                                    dictionaryContentBuilder
                                                            .append("|")
                                                            .append(dictionaryEntity.getDictionary())
                                                            .append("|")
                                                            .append(dictionaryEntity.getDictionaryDesc())
                                                            .append("|\n");
                                                });

                                        if (!dictionaryContentBuilder.isEmpty()) {
                                            dictionaryContent = """
                                                ## Reference Dictionary
                                                |word|replace_word|
                                                |---|---|
                                                """ + dictionaryContentBuilder.toString().trim();
                                        }
                                    }

                                    return translateModuleService
                                            .partTranslate(translateInfo, content, dictionaryContent, promptEntity)
                                            .map(partTranslate -> PartTranslateContextVO.of(
                                                    index,
                                                    partTranslate,
                                                    state.getStateId(),
                                                    state.increaseProgress()
                                            ));

                                }, TranslateCoreConst.BATCH_SIZE))
                .subscribeOn(Schedulers.boundedElastic())
                .cache();

        Flux<StreamEvent> translateFlux = partTranslateFlux
                .sort(Comparator.comparingInt(PartTranslateContextVO::getIndex))
                .doOnNext(partTranslateContext -> answerAccumulator.append(partTranslateContext.getPartTranslate()))
                .flatMapSequential(partTranslateContext ->
                        Flux.fromStream(partTranslateContext.getPartTranslate().codePoints().mapToObj(cp -> new String(Character.toChars(cp))))
                                .delayElements(Duration.ofMillis(1))
                                .map(content -> StreamEvent.answer(partTranslateState.getStateId(), content))
                );

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
