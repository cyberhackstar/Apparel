package com.ladiesapparel.coupon;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.cart.CartService;
import com.ladiesapparel.cart.dto.CartResponse;
import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.coupon.dto.ApplyCouponRequest;
import com.ladiesapparel.coupon.dto.CouponApplyResponse;
import com.ladiesapparel.coupon.dto.CouponRequest;
import com.ladiesapparel.coupon.dto.CouponResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequiredArgsConstructor
public class CouponController {

    private final CouponService couponService;
    private final CartService cartService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    // ---------- Admin endpoints ----------

    @PostMapping("/api/admin/coupons")
    public ResponseEntity<ApiResponse<CouponResponse>> create(@Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon created", couponService.createCoupon(request)));
    }

    @PutMapping("/api/admin/coupons/{id}")
    public ResponseEntity<ApiResponse<CouponResponse>> update(@PathVariable Long id,
                                                               @Valid @RequestBody CouponRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Coupon updated", couponService.updateCoupon(id, request)));
    }

    @DeleteMapping("/api/admin/coupons/{id}")
    public ResponseEntity<ApiResponse<Object>> deactivate(@PathVariable Long id) {
        couponService.deactivateCoupon(id);
        return ResponseEntity.ok(ApiResponse.success("Coupon deactivated"));
    }

    @GetMapping("/api/admin/coupons")
    public ResponseEntity<ApiResponse<List<CouponResponse>>> listAll() {
        return ResponseEntity.ok(ApiResponse.success("Coupons fetched", couponService.getAllCoupons()));
    }

    // ---------- Customer-facing: apply/preview on the current cart ----------

    @PostMapping("/api/cart/apply-coupon")
    public ResponseEntity<ApiResponse<CouponApplyResponse>> applyToCart(@Valid @RequestBody ApplyCouponRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        CartResponse cart = cartService.getCart();

        BigDecimal subtotal = cart.getSubtotal();
        BigDecimal discount = couponService.validateAndComputeDiscount(request.getCode(), user, subtotal);

        CouponApplyResponse response = CouponApplyResponse.builder()
                .code(request.getCode().trim().toUpperCase())
                .discountAmount(discount)
                .subtotal(subtotal)
                .payableAmount(subtotal.subtract(discount))
                .build();

        return ResponseEntity.ok(ApiResponse.success("Coupon applied", response));
    }
}
