package com.genai.core.config;

import com.genai.core.config.properties.LlmProperty;
import com.genai.core.repository.ModelRepository;
import com.genai.core.repository.impl.VllmModelRepositoryImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@RequiredArgsConstructor
public class LlmModelConfig {

    private final LlmProperty llmProperty;

    @Bean
    public ModelRepository modelRepository(
            VllmModelRepositoryImpl vllmModelRepositoryImpl
    ) {
        if (llmProperty.getPlatform().startsWith("vllm")) {
            return vllmModelRepositoryImpl;
        }

        throw new IllegalArgumentException("지원 되지 않는 모델명 : ( vllm ) 만 지원 / ( hugging-tgi | ollama | lm-studio ) 지원 예정");
    }
}
