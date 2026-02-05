package com.genai.core.service.business.impl;

import com.genai.core.config.properties.FileProperty;
import com.genai.core.exception.NotFoundException;
import com.genai.core.exception.TranslateErrorException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.DictionaryRepository;
import com.genai.core.repository.entity.AnswerEntity;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.DictionaryEntity;
import com.genai.core.service.business.TranslateCoreService;
import com.genai.core.service.business.constant.StreamCoreConst;
import com.genai.core.service.business.constant.TranslateCoreConst;
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.PrepareVO;
import com.genai.core.service.business.vo.TranslateVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.TranslateModuleService;
import com.genai.global.utils.AhoCorasick;
import com.genai.global.utils.CommonUtil;
import com.genai.global.utils.ExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateCoreServiceImpl implements TranslateCoreService {

    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final DictionaryRepository dictionaryRepository;
    private final ExtractUtil extractUtil;
    private final FileProperty fileProperty;
    private final TranslateModuleService translateModuleService;
    private final ChatHistoryModuleService chatHistoryModuleService;

//    private final AhoCorasick ahoCorasick = new AhoCorasick();
//
//    @EventListener(ApplicationReadyEvent.class)
//    public void init() {
//        dictionaryRepository.findAll().forEach(dictionaryEntity -> {
//            this.ahoCorasick.insert(dictionaryEntity.getDictionary().toLowerCase());
//        });
//        this.ahoCorasick.build();
//    }

    /**
     * 파일 번역
     *
     * @param beforeLang 원문 언어 코드
     * @param afterLang  번역 언어 코드
     * @param file       문서 파일
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @param containDic 사전 포함 여부
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String beforeLang, String afterLang, MultipartFile file, String sessionId, long chatId, boolean containDic) {

        List<String> contents = new ArrayList<>();

        String originFileName = file.getOriginalFilename();
        String fileName = CommonUtil.generateRandomId();
        Path fullPath = Paths.get(fileProperty.getFileStorePath(), fileProperty.getTempDir(), fileName);

        if (fullPath.toFile().exists()) {
            throw new TranslateErrorException(originFileName);
        }

        try {
            file.transferTo(fullPath);

            String content = extractUtil.extract(fullPath.toString());
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

        } catch (IOException e) {
            throw new TranslateErrorException(originFileName);
        } finally {
            extractUtil.removeFile(fullPath);
        }

        return this.translate(beforeLang, afterLang, contents, sessionId, chatId, containDic);
    }

    /**
     * 텍스트 번역
     *
     * @param beforeLang 원문 언어 코드
     * @param afterLang  번역 언어 코드
     * @param content    사용자 입력 텍스트
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String beforeLang, String afterLang, String content, String sessionId, long chatId, boolean containDic) {

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

        return this.translate(beforeLang, afterLang, contents, sessionId, chatId, containDic);
    }

    /**
     * 텍스트 번역
     *
     * @param beforeLang 원문 언어 코드
     * @param afterLang  번역 언어 코드
     * @param contents   사용자 입력 텍스트 목록
     * @param sessionId  세션 ID
     * @param chatId     대화 정보 ID
     * @return 번역 결과 문자열
     */
    @Override
    public TranslateVO translate(String beforeLang, String afterLang, List<String> contents, String sessionId, long chatId, boolean containDic) {

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(String.format("Translate %s to %s", beforeLang, afterLang))
                .build());

        // 사전 색인 생성
        AhoCorasick ahoCorasick = new AhoCorasick();
        List<DictionaryEntity> dictionaryEntities = new ArrayList<>();
        if (containDic) {
            dictionaryEntities.addAll(dictionaryRepository.findAll());
            dictionaryEntities.forEach(dictionaryEntity -> ahoCorasick.insert(dictionaryEntity.getDictionary().toLowerCase()));
            ahoCorasick.build();
        }

        Flux<StreamEvent> answerStream = Flux.create(sink -> {
            // 답변
            StringBuilder answerAccumulator = new StringBuilder();
            AtomicReference<Float> progressAtomic = new AtomicReference<>(0f);
            float interval = 1f / contents.size();

            Disposable disposable = Flux.fromIterable(contents)
                    .doOnSubscribe(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(Math.min(progressAtomic.get(), 1f))
                            .message("부분 번역 시작")
                            .build())))
                    .buffer(TranslateCoreConst.CHUNK_PART_BATCH_SIZE)
                    .concatMap(batch -> Flux.fromIterable(batch)
                            .flatMapSequential(content -> {
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

                                return translateModuleService.partTranslate(beforeLang, afterLang, content, dictionaryContent)
                                        .doOnNext(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                                                .progress(Math.min(progressAtomic.updateAndGet(progress -> progress + interval), 1f))
                                                .message("부분 번역 진행중")
                                                .build())));

                            } , TranslateCoreConst.CHUNK_PART_BATCH_SIZE))
                    .collectList()
                    .flatMapMany(partTranslates -> Flux.fromIterable(partTranslates)
                            .flatMapSequential(partTranslate ->
                                    Flux.fromStream(partTranslate.codePoints().mapToObj(cp -> new String(Character.toChars(cp))))
                                            .delayElements(Duration.ofMillis(1))
                                            .map(content -> AnswerEntity.builder()
                                                    .id(sessionId)
                                                    .content(content)
                                                    .finishReason(null)
                                                    .isInference(false)
                                                    .build())))
                    .doOnNext(answerEntity -> {
                        if (!answerEntity.getIsInference()) {
                            answerAccumulator.append(answerEntity.getContent());
                        }
                    })
                    .map(answerEntity -> StreamEvent.builder()
                            .id(answerEntity.getId())
                            .content(answerEntity.getContent())
                            .event(answerEntity.getIsInference() ? StreamCoreConst.Event.INFERENCE : StreamCoreConst.Event.ANSWER)
                            .build())
                    .doOnNext(sink::next)
                    .doOnComplete(() -> {
                        // 대화 이력 업데이트
                        chatHistoryModuleService.updateChatDetail(
                                chatId,
                                chatDetailEntity.getMsgId(),
                                "",
                                answerAccumulator.toString().trim(),
                                Collections.emptyList()
                        );
                        sink.complete();
                    })
                    .doOnError(sink::error)
                    .subscribe();

            sink.onCancel(disposable);
        });

        return TranslateVO.builder()
                .answerStream(answerStream)
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}
