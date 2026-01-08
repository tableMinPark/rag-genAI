package com.genai.core.service.business.impl;

import com.genai.core.config.properties.FileProperty;
import com.genai.core.constant.PromptConst;
import com.genai.core.constant.ReportConst;
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
import java.util.ArrayList;
import java.util.List;
import java.util.stream.IntStream;

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
     * @param files         문서 파일
     * @param sessionId     사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, MultipartFile[] files, String sessionId, long chatId) {

        List<String> contents = new ArrayList<>();

        for (MultipartFile file : files) {
            String originFileName = file.getOriginalFilename();
            String fileName = CommonUtil.generateRandomId();
            Path fullPath = Paths.get(fileProperty.getFileStorePath(), fileProperty.getTempDir(), fileName);

            if (fullPath.toFile().exists()) {
                throw new ReportErrorException(originFileName);
            }

            try {
                file.transferTo(fullPath);

                String fileContent = extractUtil.extract(fullPath.toString());

                int step = ReportConst.REPORT_PART_TOKEN_SIZE - ReportConst.REPORT_PART_OVERLAP_SIZE;
                List<String> fileContents = IntStream.iterate(0, i -> i + step)
                        .limit((fileContent.length() + step - 1) / step)
                        .mapToObj(i -> fileContent.substring(i, Math.min(fileContent.length(), i + ReportConst.REPORT_PART_TOKEN_SIZE)))
                        .toList();

                contents.addAll(fileContents);

            } catch (IOException e) {
                throw new ReportErrorException(originFileName);
            } finally {
                extractUtil.removeFile(fullPath);
            }
        }

        contents = contents.subList(0, Math.min(contents.size(), ReportConst.REPORT_PART_MAX_COUNT));

        return this.generateReport(reportTitle, promptContext, contents, sessionId, chatId);
    }

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param contents      참고 문서
     * @param sessionId     사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, List<String> contents, String sessionId, long chatId) {

        // 부분 요약
        StringBuilder reportPartSummariesBuilder = new StringBuilder();
        for (String content : contents) {
            PromptEntity reportPartSummaryPromptEntity = PromptEntity.builder()
                    .promptContent(ReportConst.REPORT_PART_SUMMARIES_PROMPT)
                    .temperature(ReportConst.REPORT_PART_SUMMARIES_TEMPERATURE)
                    .topP(ReportConst.REPORT_PART_SUMMARIES_TOP_P)
                    .build();

            String reportPartSummary = modelRepository.generateAnswerStr("", content, sessionId, reportPartSummaryPromptEntity);
            reportPartSummariesBuilder.append(reportPartSummary).append("\n");
        }

        // 전체 요약
        PromptEntity reportSummaryPromptEntity = PromptEntity.builder()
                .promptContent(ReportConst.REPORT_SUMMARIES_PROMPT)
                .temperature(ReportConst.REPORT_SUMMARIES_TEMPERATURE)
                .topP(ReportConst.REPORT_SUMMARIES_TOP_P)
                .build();

        String reportSummary = modelRepository.generateAnswerStr("", reportPartSummariesBuilder.toString().trim(), sessionId, reportSummaryPromptEntity);

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

        String reportContent = modelRepository.generateAnswerStr(query, reportSummary, sessionId, promptEntity);

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
