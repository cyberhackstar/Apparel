package com.ladiesapparel.coupon.dto;

import com.ladiesapparel.coupon.DiscountType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CouponResponse {
    private Long id;
    private String code;
    private String description;
    private DiscountType discountType;
    private BigDecimal discountValue;
    private BigDecimal minOrderValue;
    private BigDecimal maxDiscountAmount;
    private Integer usageLimitPerUser;
    private Integer totalUsageLimit;
    private Integer totalUsedCount;
    private Instant validFrom;
    private Instant validTo;
    private boolean active;
}
