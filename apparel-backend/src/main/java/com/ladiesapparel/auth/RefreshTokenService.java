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

    @Value("${app.jwt.refresh-token-expiration-ms}")
    private long refreshTokenExpirationMs;

    @Transactional
    public String createRefreshToken(User user) {
        // one active refresh token per user — old sessions are invalidated on fresh login
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
                .orElseThrow(() -> ApiException.unauthorized("Invalid refresh token"));

        if (token.isRevoked()) {
            throw ApiException.unauthorized("This session has been logged out. Please login again.");
        }
        if (Instant.now().isAfter(token.getExpiryDate())) {
            throw ApiException.unauthorized("Session expired. Please login again.");
        }

        return token.getUser();
    }
}
