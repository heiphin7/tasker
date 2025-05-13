package com.FrontendService.controller;

import com.FrontendService.model.auth.AuthRequest;
import com.FrontendService.model.auth.AuthResponse;
import com.FrontendService.model.auth.RegisterRequest;
import com.FrontendService.model.auth.RefreshTokenRequest;
import com.FrontendService.service.AuthService;
import com.FrontendService.util.CookieUtil;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final CookieUtil cookieUtil;

    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequest request, HttpServletResponse response, Model model) {
        log.info("Поступил запрос на регистрацию");
        try {
            AuthResponse authResponse = authService.register(request);
            cookieUtil.addTokenCookie(response, authResponse.getToken(), 3600);
            cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 86400);

            return ResponseEntity.ok(authResponse);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody AuthRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.login(request);
        cookieUtil.addTokenCookie(response, authResponse.getToken(), 3600); // 1 час
        cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 86400); // 24 часа
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refreshToken(@Valid @RequestBody RefreshTokenRequest request, HttpServletResponse response) {
        AuthResponse authResponse = authService.refreshToken(request);
        cookieUtil.addTokenCookie(response, authResponse.getToken(), 3600); // 1 час
        cookieUtil.addRefreshTokenCookie(response, authResponse.getRefreshToken(), 86400); // 24 часа
        return ResponseEntity.ok(authResponse);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, HttpServletResponse response) {
        String token = cookieUtil.getTokenFromCookies(request);
        if (token != null) {
            authService.logout(token);
        }
        cookieUtil.clearTokenCookies(response);
        return ResponseEntity.ok().build();
    }
} 