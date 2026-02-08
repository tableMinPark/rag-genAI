package com.genai.core.service.business.impl;

import com.genai.core.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.business.PromptCoreService;
import com.genai.global.utils.CommonUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PromptCoreServiceImpl implements PromptCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;

    /**
     * 나만의 AI 프롬프트 생성
     *
     * @param role        전문 분야 및 역할
     * @param answerTone  답변 말투
     * @param answerStyle 답변 스타일
     * @return 생성 프롬프트 문자열
     */
    @Transactional(readOnly = true)
    @Override
    public String generateMyAiPrompt(String role, String answerTone, String answerStyle) {

        String context = String.format("""
                1. 당신은 "%s" 역할을 합니다.
                2. "%s" 톤으로 답변 합니다.
                3. "%s" 스타일로 문장을 구성하여 답변 합니다.
                """, role, answerTone, answerStyle);

        return this.generatePrompt(PromptConst.GENERATE_MYAI_PROMPT_ID, context);
    }

    /**
     * 보고서 프롬프트 생성
     *
     * @param content 사용자 입력 텍스트
     * @return 프롬프트 VO
     */
    @Transactional(readOnly = true)
    @Override
    public String generateReportPrompt(String content) {
        return this.generatePrompt(PromptConst.GENERATE_REPORT_PROMPT_ID, content);
    }

    /**
     * 프롬프트 생성
     *
     * @param promptId 생성 프롬프트 ID
     * @param content  프롬프트 생성 컨텍스트
     * @return 생성 프롬프트 문자열
     */
    @Transactional(readOnly = true)
    @Override
    public String generatePrompt(long promptId, String content) {

        String userInput = """
                "## 답변 규칙" 을 기반으로 사용자에게 적용될 시스템 프롬프트를 작성 해줘.
                """;

        String context = String.format("""
                ## 답변 규칙
                %s
                """, content);

        PromptEntity promptEntity = promptRepository.findById(promptId)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        return modelRepository.generateAnswerSyncStr(userInput, context, CommonUtil.generateRandomId(), promptEntity);
    }
}
