package com.genai.core.service.business.vo;

import com.genai.core.service.business.subscriber.StreamEvent;
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

    private long fullMsgId;

    private Flux<StreamEvent> answerStream;
}
