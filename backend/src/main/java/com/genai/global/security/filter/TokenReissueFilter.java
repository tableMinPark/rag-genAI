package com.genai.global.security.filter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.genai.global.auth.service.domain.Member;
import com.genai.global.security.utils.JwtUtil;
import lombok.*;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.lang.NonNull;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.web.filter.OncePerRequestFilter;

import javax.servlet.FilterChain;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

@RequiredArgsConstructor
public class TokenReissueFilter extends OncePerRequestFilter {

    private final JwtUtil jwtUtil;
    private final UserDetailsService userDetailsService;
    private final ObjectMapper objectMapper;
    private final String refreshTokenCookieName;

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        return !request.getRequestURI().equals("/api/auth/reissue") ||
               !HttpMethod.POST.matches(request.getMethod());
    }

    @Override
    protected void doFilterInternal(@NonNull HttpServletRequest request, @NonNull HttpServletResponse response,
                                    @NonNull FilterChain filterChain) throws IOException {
        String refreshToken = extractRefreshToken(request);

        if (refreshToken == null || !jwtUtil.isValid(refreshToken)) {
            response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
            return;
        }

        String userId = jwtUtil.getUserId(refreshToken);
        Member member = (Member) userDetailsService.loadUserByUsername(userId);

        String newAccessToken = jwtUtil.generateAccessToken(member.getUserId(), member.getRole());

        response.setStatus(HttpServletResponse.SC_OK);
        response.setContentType(MediaType.APPLICATION_JSON_VALUE);
        response.setCharacterEncoding("UTF-8");
        objectMapper.writeValue(
                response.getWriter(),
                new ReissueResponse(newAccessToken, member.getUserId(), member.getName(), member.getMenus())
        );
    }

    private String extractRefreshToken(HttpServletRequest request) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> refreshTokenCookieName.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    private static class ReissueResponse {
        private String accessToken;
        private String userId;
        private String name;
        private List<String> menus;
    }
}
