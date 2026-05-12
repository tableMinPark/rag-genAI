package com.genai.core.config.instance;

import com.genai.core.common.enums.LlmPlatformType;
import com.genai.core.config.properties.LlmInstanceProperty;
import lombok.Builder;
import lombok.Getter;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.Semaphore;

@Getter
public class LlmInstance {

    private final String instanceId;

    private final LlmPlatformType platformType;

    private final LlmInstanceProperty llmInstanceProperty;

    private final WebClient webClient;

    private final Semaphore sessionCount;

    private final Queue<String> sessionQueue = new ConcurrentLinkedDeque<>();

    @Builder
    public LlmInstance(String instanceId, LlmPlatformType platformType, LlmInstanceProperty llmInstanceProperty, WebClient webClient) {
        this.instanceId = instanceId;
        this.platformType = platformType;
        this.llmInstanceProperty = llmInstanceProperty;
        this.webClient = webClient;
        this.sessionCount = new Semaphore(this.llmInstanceProperty.getSessionCount());
    }

    public Optional<Integer> tryAcquire(String requestId) {

        if (!sessionCount.tryAcquire()) {
            return Optional.empty();
        }

        if (!sessionQueue.add(requestId)) {
            sessionCount.release();
            return Optional.empty();
        }

        return Optional.of(sessionCount.availablePermits());
    }

    public int release(String requestId) {
        sessionQueue.remove(requestId);
        sessionCount.release();
        return sessionCount.availablePermits();
    }

    public record AcquireResult(LlmInstance instance, int sessionCount) {}
}
