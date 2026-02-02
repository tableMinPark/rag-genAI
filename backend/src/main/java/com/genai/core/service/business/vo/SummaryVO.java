package com.genai.core.service.business.vo;

import lombok.*;
import reactor.core.publisher.Flux;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class SummaryVO {

    private long chatId;

    private long msgId;

    private Flux<StreamEventVO> answerStream;
}
