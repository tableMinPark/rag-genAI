package com.genai.core.service.business.impl;

import com.genai.core.constant.ComnConst;
import com.genai.core.constant.PromptConst;
import com.genai.core.config.properties.FileProperty;
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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class TranslateCoreServiceImpl implements TranslateCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final ComnCodeRepository comnCodeRepository;
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

            String fileContent = extractUtil.extract(fullPath.toString());
            String content = originFileName + "\n" + fileContent;

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

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        Map<String, ComnCodeEntity> comnCodeEntityMap = comnCodeRepository
                .findComnCodeByCodeGroupOrderBySortOrder(ComnConst.TRANSLATE_LANGUAGE_CODE_GROUP)
                .stream().collect(Collectors.toMap(ComnCodeEntity::getCode, v -> v));

        if (!comnCodeEntityMap.containsKey(beforeLang)) {
            throw new TranslateErrorException(beforeLang);
        }

        if (!comnCodeEntityMap.containsKey(afterLang)) {
            throw new TranslateErrorException(afterLang);
        }

        String beforeLangName = comnCodeEntityMap.get(beforeLang).getCodeName();
        String afterLangName = comnCodeEntityMap.get(afterLang).getCodeName();
        String query = String.format("""
        > 컨텍스트의 문서를 %s에서 %s로 번역 해줘.
        """, beforeLangName, afterLangName);

        // 사전 목록 조회
        if (containDic) {
            StringBuilder dictionaryContentBuilder = new StringBuilder("""
            ## 사내 용어집 단어 목록
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

            query = query + "\n\n" + dictionaryContentBuilder.toString().trim();
        }

        PromptEntity promptEntity = promptRepository.findById(PromptConst.TRANSLATE_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        // 번역 결과 답변
        String translateContent = modelRepository.generateAnswerStr(query, content, CommonUtil.generateRandomId(), promptEntity);

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(query)
                .answer(translateContent)
                .build());


        return TranslateVO.builder()
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .content(translateContent)
                .build();
    }
}
