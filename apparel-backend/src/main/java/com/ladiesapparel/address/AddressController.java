package com.ladiesapparel.address;

import com.ladiesapparel.address.dto.AddressRequest;
import com.ladiesapparel.address.dto.AddressResponse;
import com.ladiesapparel.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<AddressResponse>>> getMyAddresses() {
        return ResponseEntity.ok(ApiResponse.success("Addresses fetched", addressService.getMyAddresses()));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AddressResponse>> add(@Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address added", addressService.addAddress(request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AddressResponse>> update(@PathVariable Long id,
                                                                @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Address updated", addressService.updateAddress(id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Object>> delete(@PathVariable Long id) {
        addressService.deleteAddress(id);
        return ResponseEntity.ok(ApiResponse.success("Address deleted"));
    }

    @PatchMapping("/{id}/default")
    public ResponseEntity<ApiResponse<AddressResponse>> setDefault(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success("Default address updated", addressService.setDefault(id)));
    }
}
