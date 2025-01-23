package com.techstud.sch_parser.security.impl;

import com.techstud.sch_parser.security.TokenService;
import com.techstud.sch_parser.security.AuthClient;
import com.techstud.sch_parser.security.TokenManager;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@Slf4j
@RequiredArgsConstructor
public class AuthClientImpl implements AuthClient {

    private final CloseableHttpClient httpClient;
    private final TokenManager tokenManager;
    private final TokenService jwtGenerateService;

    @Value("${auth.service.url}")
    private String authServiceUrl;

    @Override
    public void authenticateService() {
        String serviceToken = jwtGenerateService.generateServiceToken();

        HttpPost httpPost = new HttpPost(authServiceUrl + "/service/auth/validate-service");
        httpPost.setHeader("Authorization", "Bearer " + serviceToken);

        executeWithRetry(() -> {
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                handleAuthResponse(response);
            } catch (IOException e) {
                log.error("Error during service authentication: {}", e.getMessage());
                throw new RuntimeException("Failed to authenticate service", e);
            }
        });
    }

    @Override
    public void refreshTokens() {
        String currentRefreshToken = String.valueOf(tokenManager.getRefreshToken());

        if (currentRefreshToken == null || currentRefreshToken.isEmpty()) {
            log.warn("Refresh token is null or empty. Re-authenticating...");
            authenticateService();
            return;
        }

        HttpPost httpPost = new HttpPost(authServiceUrl + "/service/auth/refresh-token");
        httpPost.setHeader("Authorization", "Bearer " + currentRefreshToken);

        executeWithRetry(() -> {
            try (CloseableHttpResponse response = httpClient.execute(httpPost)) {
                handleRefreshResponse(response);
            } catch (IOException e) {
                log.error("Error during token refresh: {}", e.getMessage());
                throw new RuntimeException("Failed to refresh tokens", e);
            }
        });
    }

    private void handleAuthResponse(CloseableHttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 200) {
            String accessToken = response.getFirstHeader("Access-Token").getValue();
            String refreshToken = response.getFirstHeader("Refresh-Token").getValue();
            tokenManager.updateTokens(accessToken, refreshToken);
            log.info("Service authenticated successfully.");
        } else {
            throw new RuntimeException("Authentication failed with status code: " + statusCode);
        }
    }

    private void handleRefreshResponse(CloseableHttpResponse response) throws IOException {
        int statusCode = response.getStatusLine().getStatusCode();

        if (statusCode == 200) {
            String accessToken = response.getFirstHeader("Access-Token").getValue();
            tokenManager.updateAccessToken(accessToken);
            log.info("Access token refreshed successfully.");
        } else {
            throw new RuntimeException("Token refresh failed with status code: " + statusCode);
        }
    }

    private void executeWithRetry(Runnable action) {
        for (int i = 0; i < 3; i++) {
            try {
                action.run();
                return;
            } catch (Exception e) {
                log.warn("Attempt {} failed: {}", i + 1, e.getMessage());
                if (i < 3 - 1) {
                    try {
                        Thread.sleep(2000);
                    } catch (InterruptedException ignored) {
                    }
                }
            }
        }
        throw new RuntimeException("Max retries exceeded.");
    }
}
