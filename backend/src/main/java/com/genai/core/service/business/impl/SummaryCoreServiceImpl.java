package com.genai.core.service.business.impl;

import com.genai.core.config.properties.FileProperty;
import com.genai.core.constant.PromptConst;
import com.genai.core.constant.ReportConst;
import com.genai.core.constant.StreamConst;
import com.genai.core.exception.NotFoundException;
import com.genai.core.exception.TranslateErrorException;
import com.genai.core.repository.ChatDetailRepository;
import com.genai.core.repository.ChatRepository;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.PromptRepository;
import com.genai.core.repository.entity.ChatDetailEntity;
import com.genai.core.repository.entity.ChatEntity;
import com.genai.core.repository.entity.PromptEntity;
import com.genai.core.service.business.SummaryCoreService;
import com.genai.core.service.business.vo.StreamEventVO;
import com.genai.core.service.business.vo.SummaryVO;
import com.genai.core.service.module.ChatHistoryModuleService;
import com.genai.core.service.module.SummaryModuleService;
import com.genai.global.utils.CommonUtil;
import com.genai.global.utils.ExtractUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor
public class SummaryCoreServiceImpl implements SummaryCoreService {

    private final PromptRepository promptRepository;
    private final ModelRepository modelRepository;
    private final ChatRepository chatRepository;
    private final ChatDetailRepository chatDetailRepository;
    private final ExtractUtil extractUtil;
    private final FileProperty fileProperty;
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

        List<String> contents = new ArrayList<>();

        String originFileName = file.getOriginalFilename();
        String fileName = CommonUtil.generateRandomId();
        Path fullPath = Paths.get(fileProperty.getFileStorePath(), fileProperty.getTempDir(), fileName);

        if (fullPath.toFile().exists()) {
            throw new TranslateErrorException(originFileName);
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
            throw new TranslateErrorException(originFileName);
        } finally {
            extractUtil.removeFile(fullPath);
        }

        contents = contents.subList(0, Math.min(contents.size(), ReportConst.REPORT_PART_MAX_COUNT));

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

        int step = ReportConst.REPORT_PART_TOKEN_SIZE - ReportConst.REPORT_PART_OVERLAP_SIZE;

        List<String> contents = new ArrayList<>(IntStream.iterate(0, i -> i + step)
                .limit((content.length() + step - 1) / step)
                .mapToObj(i -> content.substring(i, Math.min(content.length(), i + ReportConst.REPORT_PART_TOKEN_SIZE)))
                .toList());

        contents = contents.subList(0, Math.min(contents.size(), ReportConst.REPORT_PART_MAX_COUNT));

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

        String query = String.format("""
        > 길이 비율 값이 %.2f 이고, 컨텍스트의 문서를 길이 비율에 맞게 요약 해줘.
        """, lengthRatio);

        PromptEntity promptEntity = promptRepository.findById(PromptConst.SUMMARY_PROMPT_ID)
                .orElseThrow(() -> new NotFoundException("프롬프트"));

        ChatEntity chatEntity = chatRepository.findById(chatId)
                .orElseThrow(() -> new NotFoundException("대화 이력"));

        // 답변 이력 생성
        ChatDetailEntity chatDetailEntity = chatDetailRepository.save(ChatDetailEntity.builder()
                .chatId(chatEntity.getChatId())
                .query(query)
                .build());

        Mono<List<String>> partSummaryMono = summaryModuleService.partSummary(contents)
                .collectList()
                .cache();

        // 답변
        StringBuilder answerAccumulator = new StringBuilder();

        Flux<StreamEventVO> answerFlux = partSummaryMono
                .map(partSummaries -> String.join("\n\n--\n\n", partSummaries))
                .flatMapMany(wholeSummary -> modelRepository.generateStreamAnswerAsync(query, wholeSummary, "", Collections.emptyList(), sessionId, promptEntity))
                .doOnNext(answer -> answerAccumulator.append(answer.getContent()))
                .map(answerEntity -> StreamEventVO.builder()
                        .id(answerEntity.getId())
                        .content(answerEntity.getContent())
                        .event(answerEntity.getIsInference() ? StreamConst.Event.INFERENCE : StreamConst.Event.ANSWER)
                        .build());

        // 대화 이력 업데이트
        Mono<Void> chatHistoryMono = Mono.when(Mono.fromRunnable(() -> {
            chatHistoryModuleService.updateChatDetail(
                    chatId,
                    chatDetailEntity.getMsgId(),
                    "",
                    answerAccumulator.toString().trim(),
                    Collections.emptyList()
            );
        })).subscribeOn(Schedulers.boundedElastic());

        Flux<StreamEventVO> answerStream = answerFlux.concatWith(chatHistoryMono.then(Mono.empty()));

        return SummaryVO.builder()
                .answerStream(answerStream)
                .chatId(chatEntity.getChatId())
                .msgId(chatDetailEntity.getMsgId())
                .build();
    }
}