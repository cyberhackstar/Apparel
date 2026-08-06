package com.ladiesapparel.serviceability;

import com.ladiesapparel.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class ServiceabilityController {

    private final ServiceabilityService serviceabilityService;

    @GetMapping("/api/public/serviceability/{pincode}")
    public ResponseEntity<ApiResponse<ServiceabilityResponse>> check(@PathVariable String pincode) {
        return ResponseEntity.ok(ApiResponse.success("Serviceability checked", serviceabilityService.check(pincode)));
    }

    @PostMapping("/api/admin/serviceability")
    public ResponseEntity<ApiResponse<Object>> add(@Valid @RequestBody PincodeRequest request) {
        serviceabilityService.addPincode(request);
        return ResponseEntity.ok(ApiResponse.success("Pincode added"));
    }

    @DeleteMapping("/api/admin/serviceability/{id}")
    public ResponseEntity<ApiResponse<Object>> remove(@PathVariable Long id) {
        serviceabilityService.removePincode(id);
        return ResponseEntity.ok(ApiResponse.success("Pincode removed"));
    }

    @GetMapping("/api/admin/serviceability")
    public ResponseEntity<ApiResponse<List<ServiceablePincode>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("Pincodes fetched", serviceabilityService.getAll()));
    }
}
