package com.genai.core.repository.impl;

import com.genai.core.repository.StreamRepository;
import org.springframework.stereotype.Component;
import org.yaml.snakeyaml.emitter.Emitter;

@Component
public class StreamRepositoryImpl implements StreamRepository {

    /**
     * Emitter 생성
     *
     * @param emitterKey emitter 식별자
     * @return Emitter
     */
    @Override
    public Emitter createEmitter(String emitterKey) {
        return null;
    }

    @Override
    public Emitter findEmitterByEmitterKey(String emitterKey) {
        return null;
    }
}
