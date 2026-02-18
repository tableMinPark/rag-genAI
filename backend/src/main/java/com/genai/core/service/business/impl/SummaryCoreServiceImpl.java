package com.genai.core.service.business.impl;

import com.genai.common.utils.ExtractUtil;
import com.genai.common.utils.FileUtil;
import com.genai.common.utils.HtmlUtil;
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
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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

        int step = SummaryCoreConst.CHUNK_PART_TOKEN_SIZE - SummaryCoreConst.CHUNK_PART_OVERLAP_SIZE;

        List<String> contents = IntStream.iterate(0, i -> i + step)
                .limit((content.length() + step - 1) / step)
                .mapToObj(i -> content.substring(i, Math.min(content.length(), i + SummaryCoreConst.CHUNK_PART_TOKEN_SIZE)))
                .toList();

        contents = contents.subList(0, Math.min(contents.size(), SummaryCoreConst.CHUNK_PART_MAX_COUNT));

        return this.summary(lengthRatio, contents, sessionId, chatId);
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

        int step = SummaryCoreConst.CHUNK_PART_TOKEN_SIZE - SummaryCoreConst.CHUNK_PART_OVERLAP_SIZE;

        List<String> contents = new ArrayList<>(IntStream.iterate(0, i -> i + step)
                .limit((content.length() + step - 1) / step)
                .mapToObj(i -> content.substring(i, Math.min(content.length(), i + SummaryCoreConst.CHUNK_PART_TOKEN_SIZE)))
                .toList());

        contents = contents.subList(0, Math.min(contents.size(), SummaryCoreConst.CHUNK_PART_MAX_COUNT));

        return this.summary(lengthRatio, contents, sessionId, chatId);
    }

    /**
     * 텍스트 요약
     *
     * @param lengthRatio 요약 길이 비율
     * @param contents    사용자 입력 텍스트
     * @param sessionId   세션 ID
     * @param chatId      대화 정보 ID
     * @return 요약 결과 문자열
     */
    @Override
    public SummaryVO summary(float lengthRatio, List<String> contents, String sessionId, long chatId) {

        String query = String.format("길이 비율 값이 **%.2f** 이고, 컨텍스트의 문서를 길이 비율에 맞게 요약 해줘.", lengthRatio);
        String fullQuery = "길이 비율 값에 대한 설정은 무시하고, 컨텍스트의 문서를 요약 해줘.";

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

        Flux<StreamEvent> answerStream = Flux.create(sink -> {
            // 답변
            StringBuilder answerAccumulator = new StringBuilder();
            StringBuilder fullAnswerAccumulator = new StringBuilder();
            AtomicReference<Float> progressAtomic = new AtomicReference<>(0f);
            float interval = 1f / (contents.size() + 1);

            Mono<String> wholeSummaryMono = Flux.fromIterable(contents)
                    .doOnSubscribe(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(Math.min(progressAtomic.get(), 1f))
                            .message("부분 요약 시작")
                            .build())))
                    .buffer(SummaryCoreConst.CHUNK_PART_BATCH_SIZE)
                    .concatMap(batch -> Flux.fromIterable(batch)
                            .flatMapSequential(content -> summaryModuleService.partSummary(content)
                                    .doOnNext(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                                            .progress(Math.min(progressAtomic.updateAndGet(progress -> progress + interval), 1f))
                                            .message("부분 요약 진행중")
                                            .build()))), SummaryCoreConst.CHUNK_PART_BATCH_SIZE))
                    .collectList()
                    .flatMap(partSummaries -> summaryModuleService.wholeSummaries(partSummaries)
                            .doOnSubscribe(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                                    .progress(Math.min(progressAtomic.get(), 1f))
                                    .message("전체 요약 시작")
                                    .build())))
                            .doOnNext(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                                    .progress(Math.min(progressAtomic.updateAndGet(progress -> progress + interval), 1f))
                                    .message("전체 요약 완료")
                                    .build()))))
                    .cache();

            // 간단 요약
            Flux<StreamEvent> summaryFlux = wholeSummaryMono
                    .flatMapMany(wholeSummary -> modelRepository.generateStreamAnswerAsync(query, wholeSummary, "", Collections.emptyList(), sessionId, promptEntity))
                    .filter(answerEntity -> !answerEntity.getIsInference())
                    .doOnNext(answerEntity -> answerAccumulator.append(answerEntity.getContent()))
                    .map(answerEntity -> StreamEvent.answer(answerEntity.getId(), SummaryResultVO.ratio(answerEntity.getContent())))
                    .doOnNext(sink::next);

            // 상세 요약
            Flux<StreamEvent> fullSummaryFlux = wholeSummaryMono
                    .flatMapMany(wholeSummary -> modelRepository.generateStreamAnswerAsync(fullQuery, wholeSummary, "", Collections.emptyList(), sessionId, promptEntity))
                    .filter(answerEntity -> !answerEntity.getIsInference())
                    .doOnNext(answerEntity -> fullAnswerAccumulator.append(answerEntity.getContent()))
                    .map(answerEntity -> StreamEvent.answer(answerEntity.getId(), SummaryResultVO.full(answerEntity.getContent())))
                    .doOnNext(sink::next);

            Disposable disposable = Flux.merge(summaryFlux, fullSummaryFlux)
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
                        sink.complete();
                    })
                    .doOnError(sink::error)
                    .subscribe();

            sink.onCancel(disposable);
        });

        return SummaryVO.builder()
                .answerStream(answerStream)
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .fullMsgId(fullChatDetailEntity.getMsgId())
                .build();
    }
}