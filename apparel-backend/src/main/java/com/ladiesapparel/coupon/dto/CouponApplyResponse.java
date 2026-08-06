package com.ladiesapparel.coupon.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponApplyResponse {
    private String code;
    private BigDecimal discountAmount;
    private BigDecimal subtotal;
    private BigDecimal payableAmount; // subtotal - discountAmount (before shipping)
}
