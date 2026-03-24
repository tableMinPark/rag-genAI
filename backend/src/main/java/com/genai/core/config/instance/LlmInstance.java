package com.genai.core.config.instance;

import com.genai.core.config.properties.LlmInstanceProperty;
import com.genai.core.type.LlmPlatformType;
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

    private final Semaphore semaphore;

    private final Queue<String> sessionQueue = new ConcurrentLinkedDeque<>();

    @Builder
    public LlmInstance(String instanceId, LlmPlatformType platformType, LlmInstanceProperty llmInstanceProperty, WebClient webClient) {
        this.instanceId = instanceId;
        this.platformType = platformType;
        this.llmInstanceProperty = llmInstanceProperty;
        this.webClient = webClient;
        this.semaphore = new Semaphore(this.llmInstanceProperty.getSessionCount());
    }

    public Optional<Integer> tryAcquire(String requestId) {

        if (!semaphore.tryAcquire()) {
            return Optional.empty();
        }

        if (!sessionQueue.add(requestId)) {
            semaphore.release(); // 롤백
            return Optional.empty();
        }

        return Optional.of(semaphore.availablePermits());
    }

    public int release(String requestId) {
        sessionQueue.remove(requestId);
        semaphore.release();
        return semaphore.availablePermits();
    }

    public record AcquireResult(LlmInstance instance, int sessionCount) { }
}
