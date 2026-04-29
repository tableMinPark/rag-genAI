package com.genai.global.service;

import com.genai.global.controller.dto.request.RegisterRequestDto;

public interface AuthService {
    void register(RegisterRequestDto requestDto);
}
