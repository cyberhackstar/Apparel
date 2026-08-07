package com.ladiesapparel.auth;

import com.ladiesapparel.auth.dto.*;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.email.EmailService;
import com.ladiesapparel.otp.OtpPurpose;
import com.ladiesapparel.otp.OtpService;
import com.ladiesapparel.security.JwtUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtUtil jwtUtil;
    private final OtpService otpService;
    private final EmailService emailService;
    private final RefreshTokenService refreshTokenService;

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCKOUT_MINUTES = 15;

    @Transactional
    public void register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw ApiException.conflict("An account with this email already exists");
        }

        User user = User.builder()
                .fullName(request.getFullName())
                .email(request.getEmail())
                .password(passwordEncoder.encode(request.getPassword()))
                .phone(request.getPhone())
                .role(Role.CUSTOMER)
                .enabled(false)
                .build();

        userRepository.save(user);

        String otp = otpService.generateOtp(request.getEmail(), OtpPurpose.REGISTER);
        emailService.sendOtpEmail(request.getEmail(), otp, "Account Verification");
    }

    @Transactional
    public void verifyRegistrationOtp(VerifyOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        if (user.isEnabled()) {
            throw ApiException.badRequest("Account already verified. Please login.");
        }

        otpService.validateOtp(request.getEmail(), request.getOtp(), OtpPurpose.REGISTER);

        user.setEnabled(true);
        userRepository.save(user);
    }

    public void resendOtp(ResendOtpRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        OtpPurpose purpose = OtpPurpose.valueOf(request.getPurpose().toUpperCase());

        String otp = otpService.generateOtp(user.getEmail(), purpose);
        String label = purpose == OtpPurpose.REGISTER ? "Account Verification" : "Password Reset";
        emailService.sendOtpEmail(user.getEmail(), otp, label);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.unauthorized("Invalid email or password"));

        if (user.isBlocked()) {
            throw ApiException.unauthorized("Your account has been blocked. Please contact support.");
        }

        if (user.getLockedUntil() != null && Instant.now().isBefore(user.getLockedUntil())) {
            long minutesLeft = ChronoUnit.MINUTES.between(Instant.now(), user.getLockedUntil()) + 1;
            throw ApiException.unauthorized(
                    "Too many failed attempts. Please try again in " + minutesLeft + " minute(s).");
        }

        if (!user.isEnabled()) {
            throw ApiException.badRequest("Please verify your email before logging in");
        }

        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword()));
        } catch (BadCredentialsException ex) {
            registerFailedLoginAttempt(user);
            throw ex; // handled globally -> "Invalid email or password"..
        }

        // successful login — reset lockout counters
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);

        String accessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        String refreshToken = refreshTokenService.createRefreshToken(user);

        return buildAuthResponse(user, accessToken, refreshToken);
    }

    public AuthResponse refreshAccessToken(RefreshTokenRequest request) {
        User user = refreshTokenService.verifyAndRotate(request.getRefreshToken());
        String newAccessToken = jwtUtil.generateToken(user.getEmail(), user.getRole().name());
        // keep the same refresh token alive until it naturally expires (simple rotation
        // policy)
        return buildAuthResponse(user, newAccessToken, request.getRefreshToken());
    }

    public void forgotPassword(ForgotPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("No account found with this email"));

        String otp = otpService.generateOtp(user.getEmail(), OtpPurpose.FORGOT_PASSWORD);
        emailService.sendOtpEmail(user.getEmail(), otp, "Password Reset");
    }

    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> ApiException.notFound("User not found"));

        otpService.validateOtp(request.getEmail(), request.getOtp(), OtpPurpose.FORGOT_PASSWORD);

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        // a password reset is a good moment to also clear any lockout state..
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
    }

    private void registerFailedLoginAttempt(User user) {
        int attempts = user.getFailedLoginAttempts() + 1;
        user.setFailedLoginAttempts(attempts);
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            user.setLockedUntil(Instant.now().plus(LOCKOUT_MINUTES, ChronoUnit.MINUTES));
        }
        userRepository.save(user);
    }

    private AuthResponse buildAuthResponse(User user, String accessToken, String refreshToken) {
        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .userId(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .role(user.getRole().name())
                .build();
    }
}
