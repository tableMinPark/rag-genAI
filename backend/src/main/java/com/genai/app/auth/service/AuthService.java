package com.genai.app.auth.service;

import com.genai.app.auth.controller.dto.request.RegisterRequestDto;

public interface AuthService {
    void register(RegisterRequestDto requestDto);
}
