package com.genai.core.service.business.impl;

import com.genai.core.config.properties.FileProperty;
import com.genai.core.constant.PromptConst;
import com.genai.core.constant.ReportConst;
import com.genai.core.constant.StreamConst;
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
import com.genai.core.service.business.subscriber.StreamEvent;
import com.genai.core.service.business.vo.PrepareVO;
import com.genai.core.service.business.vo.ReportVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.global.utils.CommonUtil;
import com.genai.global.utils.ExtractUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.Disposable;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
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
    private final SummaryModuleService summaryModuleService;
    private final ChatHistoryModuleService chatHistoryModuleService;

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

                String content = extractUtil.extract(fullPath.toString());

                int step = ReportConst.REPORT_PART_TOKEN_SIZE - ReportConst.REPORT_PART_OVERLAP_SIZE;

                contents.addAll(IntStream.iterate(0, i -> i + step)
                        .limit((content.length() + step - 1) / step)
                        .mapToObj(i -> content.substring(i, Math.min(content.length(), i + ReportConst.REPORT_PART_TOKEN_SIZE)))
                        .toList());

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
     * @param content      참고 문서
     * @param sessionId     사용자 ID
     * @param chatId        대화 정보 ID
     * @return 보고서 문자열
     */
    @Transactional
    @Override
    public ReportVO generateReport(String reportTitle, String promptContext, String content, String sessionId, long chatId) {

        int step = ReportConst.REPORT_PART_TOKEN_SIZE - ReportConst.REPORT_PART_OVERLAP_SIZE;

        List<String> contents = new ArrayList<>(IntStream.iterate(0, i -> i + step)
                .limit((content.length() + step - 1) / step)
                .mapToObj(i -> content.substring(i, Math.min(content.length(), i + ReportConst.REPORT_PART_TOKEN_SIZE)))
                .toList());

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

        String query = String.format("""
        > 보고서 생성 참고 사항 및 보고서 제목, 컨텍스트를 기반으로 보고서 생성해줘
        # 보고서 제목
        %s
        # 보고서 생성 참고 사항
        %s
        """, reportTitle, promptContext);

        PromptEntity promptEntity = promptRepository.findById(PromptConst.REPORT_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        Flux<StreamEvent> answerStream = Flux.create(sink -> {

            // 답변
            StringBuilder answerAccumulator = new StringBuilder();
            AtomicReference<Float> progressAtomic = new AtomicReference<>(0f);
            float interval = 1f / (contents.size() + 1);

            Disposable disposable = Flux.fromIterable(contents)
                    .doOnSubscribe(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                            .progress(Math.min(progressAtomic.get(), 1f))
                            .message("부분 요약 시작")
                            .build())))
                    .buffer(3)
                    .concatMap(batch -> Flux.fromIterable(batch)
                            .flatMapSequential(content -> summaryModuleService.partSummary(content)
                                    .doOnNext(s -> sink.next(StreamEvent.prepare(sessionId, PrepareVO.builder()
                                            .progress(Math.min(progressAtomic.updateAndGet(progress -> progress + interval), 1f))
                                            .message("부분 요약 진행중")
                                            .build()))), 3))
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
                    .flatMapMany(wholeSummary -> modelRepository.generateStreamAnswerAsync(query, wholeSummary, "", Collections.emptyList(), sessionId, promptEntity))
                    .doOnNext(answerEntity -> {
                        if (answerEntity.getIsInference()) {
                            answerAccumulator.append(answerEntity.getContent());
                        }
                    })
                    .map(answerEntity -> StreamEvent.builder()
                            .id(answerEntity.getId())
                            .content(answerEntity.getContent())
                            .event(answerEntity.getIsInference() ? StreamConst.Event.INFERENCE : StreamConst.Event.ANSWER)
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

        return ReportVO.builder()
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .answerStream(answerStream)
                .build();
    }
}
