package com.ladiesapparel.auth;

import com.ladiesapparel.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-token-expiration-ms:604800000}") // Default 7 days
    private long refreshTokenExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        refreshTokenRepository.revokeAllForUser(user.getId());

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .token(UUID.randomUUID().toString())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .revoked(false)
                .build();

        refreshTokenRepository.save(token);
        return token.getToken();
    }

    @Transactional
    public User verifyAndRotate(String rawToken) {
        RefreshToken token = refreshTokenRepository.findByToken(rawToken)
                .orElseThrow(() -> ApiException.unauthorized("Invalid or expired session. Please log in again."));

        if (token.isRevoked()) {
            refreshTokenRepository.delete(token);
            throw ApiException.unauthorized("This session has been revoked. Please log in again.");
        }

        if (Instant.now().isAfter(token.getExpiryDate())) {
            refreshTokenRepository.delete(token);
            throw ApiException.unauthorized("Your session has expired. Please log in again.");
        }

        return token.getUser();
    }
}