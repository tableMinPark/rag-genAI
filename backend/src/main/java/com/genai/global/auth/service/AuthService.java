package com.genai.global.auth.service;

import com.genai.global.auth.controller.dto.request.RegisterRequestDto;

public interface AuthService {

    void register(RegisterRequestDto requestDto);
}
