package com.genai.core.service.business.impl;

import com.genai.global.utils.ExtractUtil;
import com.genai.global.utils.FileUtil;
import com.genai.global.utils.HtmlUtil;
import com.genai.global.utils.StringUtil;
import com.genai.global.vo.UploadFileVO;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.business.ReportCoreService;
import com.genai.core.service.business.constant.ReportCoreConst;
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.PrepareVO;
import com.genai.core.service.business.vo.ReportVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.core.service.module.vo.PartExportContextVO;
import com.genai.core.service.module.vo.PartExportState;
import com.genai.core.utils.ReactiveLogUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class ReportCoreServiceImpl implements ReportCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final SummaryModuleService summaryModuleService;
    private final ChatHistoryModuleService chatHistoryModuleService;

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param files         문서 파일
     * @param userId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, MultipartFile[] files, String userId, long chatId) {

        List<String> contents = new ArrayList<>();

        for (MultipartFile file : files) {
            UploadFileVO uploadFile = FileUtil.uploadFileTemp(file);

            String extractContent = ExtractUtil.extractText(uploadFile.getUrl(), uploadFile.getExt());
            String content = HtmlUtil.convertTableHtmlToMarkdown(extractContent);

            contents.addAll(StringUtil.tokenize(content, ReportCoreConst.CHUNK_PART_TOKEN_SIZE));
        }

        return this.generateReport(reportTitle, promptContext, contents, userId, chatId);
    }

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param content       참고 문서
     * @param userId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, String content, String userId, long chatId) {

        List<String> contents = StringUtil.tokenize(content, ReportCoreConst.CHUNK_PART_TOKEN_SIZE);

        return this.generateReport(reportTitle, promptContext, contents, userId, chatId);
    }

    /**
     * 보고서 생성
     *
     * @param reportTitle   보고서 제목
     * @param promptContext 내용 (작성 시 요구 사항)
     * @param contents      참고 문서
     * @param userId        사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, List<String> contents, String userId, long chatId) {

        String query = String.format("""
                # 보고서 생성 참고 정보
                - 보고서 제목: %s
                """, reportTitle);

        PromptEntity promptEntity = PromptEntity.builder()
                .promptContent(promptContext)
                .temperature(ReportCoreConst.REPORT_TEMPERATURE)
                .topP(ReportCoreConst.REPORT_TOP_P)
                .build()
                .copyAndConcatPromptContent(ReportCoreConst.REPORT_GENERATE_INFO_PROMPT(reportTitle));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        Flux<PartExportContextVO> partExportFlux = summaryModuleService.partExport(PartExportState.init(contents)).cache();

        Mono<String> wholePartExportMono = partExportFlux
                .filter(PartExportContextVO::isLast)
                .sort(Comparator.comparingInt(PartExportContextVO::getIndex))
                .collectList()
                .map(partExportContexts -> {
                    StringBuilder partAccumulator = new StringBuilder();

                    for (PartExportContextVO partExportContext : partExportContexts) {
                        partAccumulator
                                .append("# 핵심 부분 추출(").append(partExportContext.getIndex()).append(")\n")
                                .append(partExportContext.getPartExport());

                        if (partExportContext.getIndex() < partExportContexts.size() - 1) {
                            partAccumulator.append("\n\n---\n\n");
                        }
                    }

                    return partAccumulator.toString().trim();
                })
                .map(wholePartExport -> wholePartExport.substring(0, Math.min(wholePartExport.length(), ReportCoreConst.CHUNK_MAX_TOKEN_SIZE)))
                .doOnEach(ReactiveLogUtil.info(ReactiveLogUtil.Message.WHOLE_PART_EXPORT_MESSAGE, v -> new Object[]{
                        StringUtil.writeJson(contents), v
                }))
                .cache();

        Flux<StreamEvent> reportFlux = wholePartExportMono
                .flatMapMany(wholeSummary -> modelRepository.generateStreamAnswerAsync(query, wholeSummary, "", Collections.emptyList(), promptEntity))
                .filter(answerEntity -> !answerEntity.getIsInference())
                .doOnNext(answerEntity -> answerAccumulator.append(answerEntity.getContent()))
                .map(answerEntity -> StreamEvent.answer(answerEntity.getId(), answerEntity.getContent()));

        Flux<StreamEvent> partExportProgressFlux = Flux.concat(
                Flux.just(StreamEvent.prepare(StringUtil.generateRandomId(), PrepareVO.builder()
                        .progress(0)
                        .message("문서 전처리중")
                        .build())),
                partExportFlux.map(PartExportContextVO::getStreamEvent)
        );

        Flux<StreamEvent> answerStream = Flux.concat(partExportProgressFlux, reportFlux)
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

        return ReportVO.builder()
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .answerStream(answerStream)
                .build();
    }
}
