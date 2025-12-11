package com.genai.core.repository;

import org.yaml.snakeyaml.emitter.Emitter;

public interface StreamRepository {

    /**
     * Emitter 생성
     *
     * @param emitterKey emitter 식별자
     * @return Emitter
     */
    Emitter createEmitter(String emitterKey);

    /**
     * Emitter 조회
     *
     * @param emitterKey emitter 식별자
     * @return Emitter
     */
    Emitter findEmitterByEmitterKey(String emitterKey);
}