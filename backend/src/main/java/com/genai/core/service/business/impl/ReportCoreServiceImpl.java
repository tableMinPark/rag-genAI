package com.genai.core.service.business.impl;

import com.genai.core.constant.PromptConst;
import com.genai.core.config.properties.FileProperty;
import com.genai.core.exception.NotFoundException;
import com.genai.core.exception.ReportErrorException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.business.ReportCoreService;
import com.genai.core.service.business.vo.ReportVO;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCoreServiceImpl implements ReportCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ExtractUtil extractUtil;
    private final FileProperty fileProperty;

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param file          문서 파일
     * @param sessionId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, MultipartFile file, String sessionId, long chatId) {

        String originFileName = file.getOriginalFilename();
        String fileName = CommonUtil.generateRandomId();
        Path fullPath = Paths.get(fileProperty.getFileStorePath(), fileProperty.getTempDir(), fileName);

        if (fullPath.toFile().exists()) {
            throw new ReportErrorException(originFileName);
        }

        try {
            file.transferTo(fullPath);

            String fileContent = extractUtil.extract(fullPath.toString());
            String content = originFileName + "\n" + fileContent;

            return this.generateReport(reportTitle, promptContext, content, sessionId, chatId);

        } catch (IOException e) {
            throw new ReportErrorException(originFileName);
        } finally {
            extractUtil.removeFile(fullPath);
        }
    }

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param content       참고 문서
     * @param sessionId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, String content, String sessionId, long chatId) {

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        String query = String.format("""
        > 보고서 생성 참고 사항 및 보고서 제목, 컨텍스트를 기반으로 보고서 생성해줘
        # 보고서 제목
        %s
        # 보고서 생성 참고 사항
        %s
        """, reportTitle, promptContext);

        PromptEntity promptEntity = promptRepository.findById(PromptConst.REPORT_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        String reportContent = modelRepository.generateAnswerStr(query, content, CommonUtil.generateRandomId(), promptEntity);

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .rewriteQuery(query)
                .answer(reportContent)
                .build());

        return ReportVO.builder()
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .content(reportContent)
                .build();
    }
}
