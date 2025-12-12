package com.genai.core.service.impl;

import com.genai.core.config.constant.PromptConst;
import com.genai.core.config.constant.QuestionConst;
import com.genai.core.config.properties.EmbedProperty;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.SummaryCoreService;
import com.genai.core.service.vo.SummaryVO;
import com.genai.core.utils.CommonUtil;
import com.genai.core.utils.ExtractUtil;
import com.genai.core.exception.NotFoundException;
import com.genai.core.exception.TranslateErrorException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@RequiredArgsConstructor
public class SummaryCoreServiceImpl implements SummaryCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ExtractUtil extractUtil;
    private final EmbedProperty embedProperty;

    /**
     * 파일 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param file        문서 파일
     * @param sessionId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    @Override
    public SummaryVO summary(float lengthRatio, MultipartFile file, String sessionId, long chatId) {

        String originFileName = file.getOriginalFilename();
        String fileName = CommonUtil.generateRandomId();
        Path fullPath = Paths.get(embedProperty.getFileStorePath(), embedProperty.getTempDir(), fileName);

        if (fullPath.toFile().exists()) {
            throw new TranslateErrorException(originFileName);
        }

        try {
            file.transferTo(fullPath);

            String fileContent = extractUtil.extract(fullPath.toString());
            String content = originFileName + "\n" + fileContent;

            return this.summary(lengthRatio, content, sessionId, chatId);

        } catch (IOException e) {
            throw new TranslateErrorException(originFileName);
        } finally {
            extractUtil.removeFile(fullPath);
        }
    }

    /**
     * 텍스트 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param content     사용자 입력 텍스트
     * @param sessionId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    @Override
    public SummaryVO summary(float lengthRatio, String content, String sessionId, long chatId) {

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        String userInput = String.format("""
        > 길이 비율 값이 %.2f 이고, 컨텍스트의 문서를 길이 비율에 맞게 요약 해줘.
        """, lengthRatio);

        PromptEntity promptEntity = promptRepository.findByPromptId(PromptConst.SUMMARY_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        StringBuilder summaryContentBuilder = new StringBuilder();

        modelRepository.generateAnswer(userInput, content, CommonUtil.generateRandomId(), promptEntity).forEach(answer -> {
            if (!answer.getIsInference()) {
                summaryContentBuilder.append(answer.getContent());
            }
        });

        String summaryContent = summaryContentBuilder.toString();

        // 질의 이력 생성
        chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .speaker(sessionId)
                .content(userInput)
                .build());

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .speaker(QuestionConst.CHAT_HISTORY_SYSTEM_NAME)
                .content(summaryContent)
                .build());

        return SummaryVO.builder()
                .chatId(chatId)
                .msgId(chatDetailEntity.getMsgId())
                .content(summaryContent)
                .build();
    }
}