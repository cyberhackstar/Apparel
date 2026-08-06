package com.ladiesapparel.coupon;

import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.coupon.dto.CouponRequest;
import com.ladiesapparel.coupon.dto.CouponResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CouponService {

    private final CouponRepository couponRepository;
    private final CouponUsageRepository couponUsageRepository;

    // ---------- Admin CRUD ----------

    @Transactional
    public CouponResponse createCoupon(CouponRequest request) {
        String code = request.getCode().trim().toUpperCase();

        if (couponRepository.existsByCodeIgnoreCase(code)) {
            throw ApiException.conflict("Coupon code already exists: " + code);
        }
        if (!request.getValidTo().isAfter(request.getValidFrom())) {
            throw ApiException.badRequest("Valid-to date must be after valid-from date");
        }

        Coupon coupon = Coupon.builder()
                .code(code)
                .description(request.getDescription())
                .discountType(request.getDiscountType())
                .discountValue(request.getDiscountValue())
                .minOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO)
                .maxDiscountAmount(request.getMaxDiscountAmount())
                .usageLimitPerUser(request.getUsageLimitPerUser() != null ? request.getUsageLimitPerUser() : 1)
                .totalUsageLimit(request.getTotalUsageLimit())
                .totalUsedCount(0)
                .validFrom(request.getValidFrom())
                .validTo(request.getValidTo())
                .active(true)
                .build();

        couponRepository.save(coupon);
        return toResponse(coupon);
    }

    @Transactional
    public CouponResponse updateCoupon(Long id, CouponRequest request) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Coupon not found"));

        coupon.setDescription(request.getDescription());
        coupon.setDiscountType(request.getDiscountType());
        coupon.setDiscountValue(request.getDiscountValue());
        coupon.setMinOrderValue(request.getMinOrderValue() != null ? request.getMinOrderValue() : BigDecimal.ZERO);
        coupon.setMaxDiscountAmount(request.getMaxDiscountAmount());
        coupon.setUsageLimitPerUser(request.getUsageLimitPerUser() != null ? request.getUsageLimitPerUser() : coupon.getUsageLimitPerUser());
        coupon.setTotalUsageLimit(request.getTotalUsageLimit());
        coupon.setValidFrom(request.getValidFrom());
        coupon.setValidTo(request.getValidTo());

        couponRepository.save(coupon);
        return toResponse(coupon);
    }

    @Transactional
    public void deactivateCoupon(Long id) {
        Coupon coupon = couponRepository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Coupon not found"));
        coupon.setActive(false);
        couponRepository.save(coupon);
    }

    public List<CouponResponse> getAllCoupons() {
        return couponRepository.findAll().stream().map(this::toResponse).collect(Collectors.toList());
    }

    // ---------- Validation + discount computation (shared by preview & order placement) ----------

    /**
     * Validates the coupon against the given user and subtotal, and returns the discount amount.
     * Does NOT record usage — call recordUsage() only after the order is actually placed.
     */
    public BigDecimal validateAndComputeDiscount(String rawCode, User user, BigDecimal subtotal) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(rawCode.trim())
                .orElseThrow(() -> ApiException.badRequest("Invalid coupon code"));

        if (!coupon.isActive()) {
            throw ApiException.badRequest("This coupon is no longer active");
        }

        Instant now = Instant.now();
        if (now.isBefore(coupon.getValidFrom()) || now.isAfter(coupon.getValidTo())) {
            throw ApiException.badRequest("This coupon has expired or is not yet active");
        }

        if (subtotal.compareTo(coupon.getMinOrderValue()) < 0) {
            throw ApiException.badRequest(
                    "Minimum order value of Rs. " + coupon.getMinOrderValue() + " required for this coupon");
        }

        if (coupon.getTotalUsageLimit() != null && coupon.getTotalUsedCount() >= coupon.getTotalUsageLimit()) {
            throw ApiException.badRequest("This coupon has reached its usage limit");
        }

        long usedByThisUser = couponUsageRepository.countByCouponIdAndUserId(coupon.getId(), user.getId());
        if (usedByThisUser >= coupon.getUsageLimitPerUser()) {
            throw ApiException.badRequest("You have already used this coupon the maximum number of times");
        }

        return computeDiscount(coupon, subtotal);
    }

    @Transactional
    public void recordUsage(String rawCode, User user, String orderNumber) {
        Coupon coupon = couponRepository.findByCodeIgnoreCase(rawCode.trim())
                .orElseThrow(() -> ApiException.badRequest("Invalid coupon code"));

        coupon.setTotalUsedCount(coupon.getTotalUsedCount() + 1);
        couponRepository.save(coupon);

        CouponUsage usage = CouponUsage.builder()
                .coupon(coupon)
                .user(user)
                .orderNumber(orderNumber)
                .build();
        couponUsageRepository.save(usage);
    }

    private BigDecimal computeDiscount(Coupon coupon, BigDecimal subtotal) {
        BigDecimal discount;

        if (coupon.getDiscountType() == DiscountType.FLAT) {
            discount = coupon.getDiscountValue();
        } else {
            discount = subtotal.multiply(coupon.getDiscountValue())
                    .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP);
            if (coupon.getMaxDiscountAmount() != null && discount.compareTo(coupon.getMaxDiscountAmount()) > 0) {
                discount = coupon.getMaxDiscountAmount();
            }
        }

        // never let the discount exceed the subtotal itself
        return discount.compareTo(subtotal) > 0 ? subtotal : discount;
    }

    private CouponResponse toResponse(Coupon coupon) {
        return CouponResponse.builder()
                .id(coupon.getId())
                .code(coupon.getCode())
                .description(coupon.getDescription())
                .discountType(coupon.getDiscountType())
                .discountValue(coupon.getDiscountValue())
                .minOrderValue(coupon.getMinOrderValue())
                .maxDiscountAmount(coupon.getMaxDiscountAmount())
                .usageLimitPerUser(coupon.getUsageLimitPerUser())
                .totalUsageLimit(coupon.getTotalUsageLimit())
                .totalUsedCount(coupon.getTotalUsedCount())
                .validFrom(coupon.getValidFrom())
                .validTo(coupon.getValidTo())
                .active(coupon.isActive())
                .build();
    }
}
