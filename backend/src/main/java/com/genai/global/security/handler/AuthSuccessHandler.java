package com.genai.global.security.handler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.global.service.domain.Member;
import com.genai.global.security.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.web.authentication.AuthenticationSuccessHandler;
import org.springframework.stereotype.Component;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class AuthSuccessHandler implements AuthenticationSuccessHandler {

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;

    @Value("${jwt.refresh-token-cookie-name}")
    private String refreshTokenCookieName;

    @Override
    public void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response,
                                        Authentication authentication) throws IOException {
        Member member = (Member) authentication.getPrincipal();

        String accessToken = jwtUtil.generateAccessToken(member.getUserId(), member.getRole());
        String refreshToken = jwtUtil.generateRefreshToken(member.getUserId());

        Cookie cookie = new Cookie(refreshTokenCookieName, refreshToken);
        cookie.setHttpOnly(true);
        cookie.setPath("/");
        cookie.setMaxAge((int) (jwtUtil.getRefreshTokenExpiry() / 1000));
        response.addCookie(cookie);

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(response.getWriter(), Map.of(
                "accessToken", accessToken,
                "userId", member.getUserId(),
                "name", member.getName()
        ));
    }
}
