package com.genai.core.service.business.impl;

import com.genai.common.utils.ExtractUtil;
import com.genai.common.utils.FileUtil;
import com.genai.common.utils.HtmlUtil;
import com.genai.common.utils.StringUtil;
import com.genai.common.vo.UploadFileVO;
import com.genai.core.constant.PromptConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.business.SummaryCoreService;
import com.genai.core.service.business.constant.SummaryCoreConst;
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.PrepareVO;
import com.genai.core.service.business.vo.SummaryResultVO;
import com.genai.core.service.business.vo.SummaryVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.core.service.module.vo.PartExportContextVO;
import com.genai.core.service.module.vo.PartExportState;
import com.genai.core.utils.ReactiveLogUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SummaryCoreServiceImpl implements SummaryCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final SummaryModuleService summaryModuleService;
    private final ChatHistoryModuleService chatHistoryModuleService;

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

        UploadFileVO uploadFile = FileUtil.uploadFileTemp(file);

        String extractContent = ExtractUtil.extractText(uploadFile.getUrl(), uploadFile.getExt());
        String content = HtmlUtil.convertTableHtmlToMarkdown(extractContent);

        List<String> contents = StringUtil.tokenize(content, SummaryCoreConst.CHUNK_PART_TOKEN_SIZE);

        return this.summary(lengthRatio, contents, sessionId, chatId);
    }

    /**
     * 텍스트 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param content     사용자 입력 텍스트
     * @param userId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    @Override
    public SummaryVO summary(float lengthRatio, String content, String userId, long chatId) {

        List<String> contents = StringUtil.tokenize(content, SummaryCoreConst.CHUNK_PART_TOKEN_SIZE);

        return this.summary(lengthRatio, contents, userId, chatId);
    }

    /**
     * 텍스트 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param contents    사용자 입력 텍스트
     * @param userId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    @Override
    public SummaryVO summary(float lengthRatio, List<String> contents, String userId, long chatId) {

        String query = String.format("길이 비율 값이 **%.2f** 이고, 컨텍스트의 문서를 길이 비율에 맞게 요약하라.", lengthRatio);
        String fullQuery = "길이 비율 값에 대한 설정은 무시하고, 컨텍스트의 문서를 요약하라.";

        PromptEntity promptEntity = promptRepository.findById(PromptConst.SUMMARY_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        // 전체 요약 답변 이력 생성
        ChatDetailEntity fullChatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(fullQuery)
                .build());

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();
        StringBuilder fullAnswerAccumulator = new StringBuilder();

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
                .map(wholePartExport -> wholePartExport.substring(0, Math.min(wholePartExport.length(), SummaryCoreConst.CHUNK_MAX_TOKEN_SIZE)))
                .doOnEach(ReactiveLogUtil.info(ReactiveLogUtil.Message.WHOLE_PART_EXPORT_MESSAGE, v -> new Object[]{
                        StringUtil.writeJson(contents), v
                }))
                .cache();

        // 간단 요약
        Flux<StreamEvent> summaryFlux = wholePartExportMono
                .flatMapMany(wholePartExport -> modelRepository.generateStreamAnswerAsync(query, wholePartExport, "", Collections.emptyList(), promptEntity))
                .filter(answerEntity -> !answerEntity.getIsInference())
                .doOnNext(answerEntity -> answerAccumulator.append(answerEntity.getContent()))
                .map(answerEntity -> StreamEvent.answer(answerEntity.getId(), SummaryResultVO.ratio(answerEntity.getContent())));

        // 상세 요약
        Flux<StreamEvent> fullSummaryFlux = wholePartExportMono
                .flatMapMany(wholePartExport -> modelRepository.generateStreamAnswerAsync(fullQuery, wholePartExport, "", Collections.emptyList(), promptEntity))
                .filter(answerEntity -> !answerEntity.getIsInference())
                .doOnNext(answerEntity -> fullAnswerAccumulator.append(answerEntity.getContent()))
                .map(answerEntity -> StreamEvent.answer(answerEntity.getId(), SummaryResultVO.full(answerEntity.getContent())));

        Flux<StreamEvent> summaryMergeFlux = Flux.merge(summaryFlux, fullSummaryFlux);

        Flux<StreamEvent> partExportProgressFlux = Flux.concat(
                Flux.just(StreamEvent.prepare(StringUtil.generateRandomId(), PrepareVO.builder()
                        .progress(0)
                        .message("문서 전처리중")
                        .build())),
                partExportFlux.map(PartExportContextVO::getStreamEvent)
        );

        Flux<StreamEvent> answerStream = Flux.concat(partExportProgressFlux, summaryMergeFlux)
                .doOnCancel(() -> {
                    chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId());
                    chatHistoryModuleService.deleteChatDetail(fullChatDetailEntity.getMsgId());
                })
                .doOnError(throwable -> {
                    chatHistoryModuleService.deleteChatDetail(chatDetailEntity.getMsgId());
                    chatHistoryModuleService.deleteChatDetail(fullChatDetailEntity.getMsgId());
                })
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
                    // 대화 이력 업데이트
                    chatHistoryModuleService.updateChatDetail(
                            chatId,
                            fullChatDetailEntity.getMsgId(),
                            "",
                            fullAnswerAccumulator.toString().trim(),
                            Collections.emptyList()
                    );
                });

        return SummaryVO.builder()
                .answerStream(answerStream)
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .fullMsgId(fullChatDetailEntity.getMsgId())
                .build();
    }
}