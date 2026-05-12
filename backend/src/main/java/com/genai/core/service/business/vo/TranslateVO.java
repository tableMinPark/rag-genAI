package com.genai.core.service.business.vo;

import com.genai.global.stream.subscriber.StreamEvent;
import lombok.*;
import reactor.core.publisher.Flux;

@ToString
@Builder
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class TranslateVO {

    private long chatId;

    private long msgId;

    private Flux<StreamEvent> answerStream;
}
