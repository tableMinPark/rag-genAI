package com.genai.core.service.business.impl;

import com.genai.core.constant.ComnConst;
import com.genai.core.constant.PromptConst;
import com.genai.core.config.properties.FileProperty;
import com.genai.core.constant.TranslateConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.exception.TranslateErrorException;
import com.genai.core.repository.*;
import com.genai.core.repository.entity.*;
import com.genai.core.service.business.TranslateCoreService;
import com.genai.core.service.business.vo.TranslateVO;
import com.genai.global.utils.CommonUtil;
import com.genai.global.utils.ExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateCoreServiceImpl implements TranslateCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final CommonCodeRepository commonCodeRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final DictionaryRepository dictionaryRepository;
    private final ExtractUtil extractUtil;
    private final FileProperty fileProperty;

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
    @Transactional
    @Override
    public TranslateVO translate(String beforeLang, String afterLang, MultipartFile file, String sessionId, long chatId, boolean containDic) {

        String originFileName = file.getOriginalFilename();
        String fileName = CommonUtil.generateRandomId();
        Path fullPath = Paths.get(fileProperty.getFileStorePath(), fileProperty.getTempDir(), fileName);

        if (fullPath.toFile().exists()) {
            throw new TranslateErrorException(originFileName);
        }

        try {
            file.transferTo(fullPath);

            String content = extractUtil.extract(fullPath.toString());

            return this.translate(beforeLang, afterLang, content, sessionId, chatId, containDic);

        } catch (IOException e) {
            throw new TranslateErrorException(originFileName);
        } finally {
            extractUtil.removeFile(fullPath);
        }
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
    @Transactional
    @Override
    public TranslateVO translate(String beforeLang, String afterLang, String content, String sessionId, long chatId, boolean containDic) {

        StringBuilder translateContentBuilder = new StringBuilder();
        List<String> lines = content.lines().toList();
        Queue<List<String>> contextQueue = new ConcurrentLinkedDeque<>();

        StringBuilder contentBuilder = new StringBuilder();
        List<String> contexts = new ArrayList<>();
        for (String line : lines) {
            if (contentBuilder.length() + line.length() > TranslateConst.TRANSLATE_PART_TOKEN_SIZE) {
                contexts.add(contentBuilder.toString().trim());
                contentBuilder = new StringBuilder();
            }
            if (contexts.size() == TranslateConst.TRANSLATE_PARK_BATCH_SIZE) {
                contextQueue.offer(contexts);
                contexts = new ArrayList<>();
            }
            contentBuilder.append(line).append("\n");
        }

        if (!contentBuilder.toString().trim().isEmpty()) {
            contexts.add(contentBuilder.toString().trim());
            contextQueue.offer(contexts);
        }

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        Map<String, CommonCodeEntity> comnCodeEntityMap = commonCodeRepository
                .findComnCodeByCodeGroupOrderBySortOrder(ComnConst.TRANSLATE_LANGUAGE_CODE_GROUP)
                .stream().collect(Collectors.toMap(CommonCodeEntity::getCode, v -> v));

        if (!comnCodeEntityMap.containsKey(beforeLang)) {
            throw new TranslateErrorException(beforeLang);
        }

        if (!comnCodeEntityMap.containsKey(afterLang)) {
            throw new TranslateErrorException(afterLang);
        }

        String beforeLangName = comnCodeEntityMap.get(beforeLang).getCodeName();
        String afterLangName = comnCodeEntityMap.get(afterLang).getCodeName();
        StringBuilder queryBuilder = new StringBuilder(String.format("Translate %s to %s", beforeLangName, afterLangName));

        if (!contextQueue.isEmpty()) {
            // 사전 목록 조회
            StringBuilder dictionaryContentBuilder = new StringBuilder();
            if (containDic) {
                dictionaryContentBuilder.append("""
                        ## Reference Dictionary
                        |word|replace_word|
                        |---|---|
                        """);

                List<DictionaryEntity> dictionaryEntities = dictionaryRepository.findAll().stream()
                        // TODO: 아호 코라식 or KMP 문자열 비교 알고리즘 활용, content 내 사전 단어 포함 여부 확인 후 필터링
                        .filter(v -> {
                            return true;
                        })
                        .toList();

                dictionaryEntities.forEach(dictionaryEntity -> {
                    dictionaryContentBuilder
                            .append("|")
                            .append(dictionaryEntity.getDictionary())
                            .append("|")
                            .append(dictionaryEntity.getDictionaryDesc())
                            .append("|\n");
                });

                queryBuilder.append("\n\n").append(dictionaryContentBuilder);
            }

            PromptEntity promptEntity = promptRepository.findById(PromptConst.TRANSLATE_PROMPT_ID)
                    .orElseThrow(() -> new NotFoundException("프롬프트"));

            // 번역 결과 답변
            while (!contextQueue.isEmpty()) {
                // 병렬 처리
                contextQueue.poll().parallelStream().forEach(context -> {
                    String translateContent = modelRepository.generateAnswerStr(queryBuilder.toString(), context, CommonUtil.generateRandomId(), promptEntity);
                    translateContentBuilder.append(translateContent).append("\n");
                });
            }
        }

        String translateContent = translateContentBuilder.toString().trim();

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(queryBuilder.toString())
                .rewriteQuery(queryBuilder.toString())
                .answer(translateContent)
                .build());

        return TranslateVO.builder()
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .content(translateContent)
                .build();
    }
}
