package com.ladiesapparel.account;

import com.ladiesapparel.account.dto.*;
import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.auth.UserRepository;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.email.EmailService;
import com.ladiesapparel.otp.OtpPurpose;
import com.ladiesapparel.otp.OtpService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AccountService {

    private final UserRepository userRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;
    private final OtpService otpService;
    private final EmailService emailService;
    private final PasswordEncoder passwordEncoder;

    public ProfileResponse getProfile() {
        return toResponse(authenticatedUserProvider.getCurrentUser());
    }

    @Transactional
    public ProfileResponse updateProfile(UpdateProfileRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        user.setFullName(request.getFullName());
        user.setPhone(request.getPhone());
        userRepository.save(user);
        return toResponse(user);
    }

    /** Step 1 of email change: send an OTP to the NEW email address (never the current one). */
    public void requestEmailChange(ChangeEmailRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        if (request.getNewEmail().equalsIgnoreCase(user.getEmail())) {
            throw ApiException.badRequest("This is already your current email address");
        }
        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        String otp = otpService.generateOtp(request.getNewEmail(), OtpPurpose.CHANGE_EMAIL);
        emailService.sendOtpEmail(request.getNewEmail(), otp, "Email Change Verification");
    }

    /** Step 2 of email change: verify the OTP sent to the new address, then swap it in. */
    @Transactional
    public ProfileResponse verifyEmailChange(VerifyEmailChangeRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        if (userRepository.existsByEmail(request.getNewEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        otpService.validateOtp(request.getNewEmail(), request.getOtp(), OtpPurpose.CHANGE_EMAIL);

        user.setEmail(request.getNewEmail());
        userRepository.save(user);
        return toResponse(user);
    }

    @Transactional
    public void changePassword(ChangePasswordRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw ApiException.badRequest("Current password is incorrect");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private ProfileResponse toResponse(User user) {
        return ProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .role(user.getRole())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
