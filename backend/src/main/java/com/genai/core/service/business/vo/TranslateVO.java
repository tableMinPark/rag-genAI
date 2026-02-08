package com.genai.core.service.business.vo;

import com.genai.core.service.business.subscriber.StreamEvent;
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
