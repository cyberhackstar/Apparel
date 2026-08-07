package com.ladiesapparel.account;

import com.ladiesapparel.account.dto.*;
import com.ladiesapparel.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @GetMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> getProfile() {
        return ResponseEntity.ok(ApiResponse.success("Profile fetched", accountService.getProfile()));
    }

    @PutMapping("/profile")
    public ResponseEntity<ApiResponse<ProfileResponse>> updateProfile(@Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Profile updated", accountService.updateProfile(request)));
    }

    @PostMapping("/change-email/request")
    public ResponseEntity<ApiResponse<Object>> requestEmailChange(@Valid @RequestBody ChangeEmailRequest request) {
        accountService.requestEmailChange(request);
        return ResponseEntity.ok(ApiResponse.success("Verification code sent to your new email address"));
    }

    @PostMapping("/change-email/verify")
    public ResponseEntity<ApiResponse<ProfileResponse>> verifyEmailChange(@Valid @RequestBody VerifyEmailChangeRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Email updated successfully", accountService.verifyEmailChange(request)));
    }

    @PostMapping("/change-password")
    public ResponseEntity<ApiResponse<Object>> changePassword(@Valid @RequestBody ChangePasswordRequest request) {
        accountService.changePassword(request);
        return ResponseEntity.ok(ApiResponse.success("Password changed successfully"));
    }
}
