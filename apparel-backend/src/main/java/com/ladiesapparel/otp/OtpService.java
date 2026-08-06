package com.ladiesapparel.otp;

import com.ladiesapparel.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.security.SecureRandom;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class OtpService {

    private final OtpVerificationRepository otpRepository;

    @Value("${app.otp.expiry-minutes}")
    private int otpExpiryMinutes;

    @Value("${app.otp.length}")
    private int otpLength;

    private static final SecureRandom RANDOM = new SecureRandom();

    /**
     * Generates a new OTP, saves it, and returns the plain code (to be emailed).
     */
    public String generateOtp(String email, OtpPurpose purpose) {
        String code = generateNumericCode(otpLength);

        OtpVerification otp = OtpVerification.builder()
                .email(email)
                .otpCode(code)
                .purpose(purpose)
                .expiryTime(Instant.now().plus(otpExpiryMinutes, ChronoUnit.MINUTES))
                .verified(false)
                .attemptCount(0)
                .build();

        otpRepository.save(otp);
        return code;
    }

    /**
     * Validates the OTP entered by the user. Throws ApiException on any failure.
     */
    public void validateOtp(String email, String enteredCode, OtpPurpose purpose) {
        OtpVerification otp = otpRepository
                .findTopByEmailAndPurposeOrderByCreatedAtDesc(email, purpose)
                .orElseThrow(() -> ApiException.badRequest("No OTP request found for this email"));

        if (otp.isVerified()) {
            throw ApiException.badRequest("OTP already used. Please request a new one.");
        }

        if (Instant.now().isAfter(otp.getExpiryTime())) {
            throw ApiException.badRequest("OTP has expired. Please request a new one.");
        }

        if (otp.getAttemptCount() >= 5) {
            throw ApiException.badRequest("Too many incorrect attempts. Please request a new OTP.");
        }

        if (!otp.getOtpCode().equals(enteredCode)) {
            otp.setAttemptCount(otp.getAttemptCount() + 1);
            otpRepository.save(otp);
            throw ApiException.badRequest("Incorrect OTP");
        }

        otp.setVerified(true);
        otpRepository.save(otp);
    }

    private String generateNumericCode(int length) {
        StringBuilder sb = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            sb.append(RANDOM.nextInt(10));
        }
        return sb.toString();
    }
}
