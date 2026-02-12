package com.genai.core.type;

import com.genai.core.config.properties.LlmProperty;
import com.genai.core.repository.request.OpenAIAnswerRequest;
import com.genai.core.repository.request.VllmAnswerRequest;
import com.genai.core.repository.response.AnswerResponse;
import com.genai.core.repository.response.OpenAIAnswerResponse;
import com.genai.core.repository.response.VllmAnswerResponse;
import com.genai.core.repository.vo.ConversationVO;
import com.genai.core.utils.TokenCalculateUtil;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.util.List;

@Getter
@AllArgsConstructor
public enum LlmPlatformType {

    VLLM("vllm", VllmAnswerResponse.class),
    OPENAI("openai", OpenAIAnswerResponse.class),
    ;

    private final String platform;
    private final Class<? extends AnswerResponse> responseClass;

    public Object request(
            LlmProperty llmProperty, Double temperature, Double topP, boolean stream,
            String prompt, String query, String context, String chatState, List<ConversationVO> conversations
    ) {
        return switch (this) {
            case VLLM -> VllmAnswerRequest.builder()
                    .modelName(llmProperty.getModelName())
                    .temperature(temperature)
                    .topP(topP)
                    .maxTokens(TokenCalculateUtil.calculateMaxTokens(llmProperty, prompt, query, chatState, conversations, context))
                    .stream(stream)
                    .prompt(prompt)
                    .chatState(chatState)
                    .conversations(conversations)
                    .context(context)
                    .query(query)
                    .build();

            case OPENAI -> OpenAIAnswerRequest.builder()
                    .modelName(llmProperty.getModelName())
                    .temperature(temperature)
                    .topP(topP)
                    .maxTokens(TokenCalculateUtil.calculateMaxTokens(llmProperty, prompt, query, chatState, conversations, context))
                    .stream(stream)
                    .prompt(prompt)
                    .chatState(chatState)
                    .conversations(conversations)
                    .context(context)
                    .query(query)
                    .build();
        };
    }
}
