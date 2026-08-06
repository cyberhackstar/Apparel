package com.ladiesapparel.auth;

import com.ladiesapparel.auth.dto.CustomerResponse;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.common.PagedResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/customers")
@RequiredArgsConstructor
public class AdminCustomerController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<ApiResponse<PagedResponse<CustomerResponse>>> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize);
        var result = PagedResponse.from(userRepository.findByRole(Role.CUSTOMER, pageable).map(this::toResponse));
        return ResponseEntity.ok(ApiResponse.success("Customers fetched", result));
    }

    @PatchMapping("/{id}/block")
    public ResponseEntity<ApiResponse<Object>> block(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
        user.setBlocked(true);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Customer blocked"));
    }

    @PatchMapping("/{id}/unblock")
    public ResponseEntity<ApiResponse<Object>> unblock(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow(() -> ApiException.notFound("Customer not found"));
        user.setBlocked(false);
        user.setFailedLoginAttempts(0);
        user.setLockedUntil(null);
        userRepository.save(user);
        return ResponseEntity.ok(ApiResponse.success("Customer unblocked"));
    }

    private CustomerResponse toResponse(User user) {
        return CustomerResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .enabled(user.isEnabled())
                .blocked(user.isBlocked())
                .createdAt(user.getCreatedAt())
                .build();
    }
}
