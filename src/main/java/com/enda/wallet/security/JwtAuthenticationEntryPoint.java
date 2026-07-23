package com.enda.wallet.security;



import com.fasterxml.jackson.databind.ObjectMapper;
import com.enda.wallet.model.dto.response.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Component
@RequiredArgsConstructor
public class JwtAuthenticationEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(HttpServletRequest request, HttpServletResponse response,
                         AuthenticationException authException) throws IOException {
        response.setStatus(HttpServletResponse.SC_UNAUTHORIZED);
        response.setContentType("application/json");

        ApiResponse<?> apiResponse = ApiResponse.error(
                HttpStatus.UNAUTHORIZED.value(),
                "UNAUTHORIZED",
                "Accès non autorisé. Veuillez vous authentifier."
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }
}