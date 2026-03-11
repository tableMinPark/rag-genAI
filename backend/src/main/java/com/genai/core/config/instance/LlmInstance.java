package com.genai.core.config.instance;

import com.genai.core.config.properties.LlmInstanceProperty;
import com.genai.core.type.LlmPlatformType;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.concurrent.atomic.AtomicInteger;

@Getter
public class LlmInstance {

    private final String instanceId;

    private final LlmPlatformType platformType;

    private final LlmInstanceProperty llmInstanceProperty;

    private final WebClient webClient;

    private final AtomicInteger sessionCount;

    @Builder
    public LlmInstance(String instanceId, LlmPlatformType platformType, LlmInstanceProperty llmInstanceProperty, WebClient webClient, int sessionCount) {
        this.instanceId = instanceId;
        this.platformType = platformType;
        this.llmInstanceProperty = llmInstanceProperty;
        this.webClient = webClient;
        this.sessionCount = new AtomicInteger(sessionCount);
    }

    public boolean tryAcquire() {
        int currentCount;
        while ((currentCount = sessionCount.get()) > 0) {
            if (sessionCount.compareAndSet(currentCount, currentCount - 1)) {
                return true;
            }
        }

        return false;
    }

    public void release() {
        sessionCount.incrementAndGet();
    }
}
