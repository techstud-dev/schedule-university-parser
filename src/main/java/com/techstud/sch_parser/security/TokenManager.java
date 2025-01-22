package com.techstud.sch_parser.security;

import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicReference;

@Component
@Slf4j
@Getter
public class TokenManager {

    private final AtomicReference<String> accessToken = new AtomicReference<>();
    private final AtomicReference<String> refreshToken = new AtomicReference<>();

    public void updateTokens(String newAccessToken, String newRefreshToken) {
        accessToken.set(newAccessToken);
        refreshToken.set(newRefreshToken);
        log.info("Tokens updated. Access token: {}..., Refresh token: {}...",
                mask(newAccessToken), mask(newRefreshToken));
    }

    private String mask(String token) {
        return token == null ? "null" : token.substring(0, 5) + "*******";
    }
}
