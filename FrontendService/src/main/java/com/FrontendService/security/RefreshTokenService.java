package com.FrontendService.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.Key;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

@Service
public class RefreshTokenService {
    @Value("${jwt.refresh.secret}")
    private String refreshSecretKey;
    @Value("${jwt.refresh.expiration}")
    private long refreshExpiration;

    public String generateRefreshToken(Long userId) {
        Map<String, Object> claims = new HashMap<>();
        claims.put("userId", userId);
        
        return Jwts
                .builder()
                .setClaims(claims)
                .setIssuedAt(new Date(System.currentTimeMillis()))
                .setExpiration(new Date(System.currentTimeMillis() + refreshExpiration))
                .signWith(getSignInKey(), SignatureAlgorithm.HS256)
                .compact();
    }

    public Long validateRefreshToken(String token) {
        try {
            Claims claims = Jwts
                    .parserBuilder()
                    .setSigningKey(getSignInKey())
                    .build()
                    .parseClaimsJws(token)
                    .getBody();

            if (claims.getExpiration().before(new Date())) {
                throw new RuntimeException("Refresh токен истек");
            }

            return claims.get("userId", Long.class);
        } catch (Exception e) {
            throw new RuntimeException("Недействительный refresh токен");
        }
    }

    public void invalidateRefreshToken(Long userId) {
        // В данном случае мы просто не храним токены на сервере
        // В реальном приложении здесь можно добавить токен в черный список
    }

    private Key getSignInKey() {
        return Keys.hmacShaKeyFor(refreshSecretKey.getBytes());
    }
} 