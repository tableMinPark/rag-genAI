package com.genai.core.service;

import org.yaml.snakeyaml.emitter.Emitter;

public interface StreamCoreService {

    /**
     * Emitter 생성 및 조회
     *
     * @param emitterKey emitter 식별자
     * @return Emitter
     */
    Emitter createEmitter(String emitterKey);
}
