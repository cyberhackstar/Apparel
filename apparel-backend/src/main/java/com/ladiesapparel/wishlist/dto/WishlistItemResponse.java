package com.ladiesapparel.wishlist.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WishlistItemResponse {
    private Long productId;
    private String name;
    private String slug;
    private String imageUrl;
    private BigDecimal basePrice;
    private BigDecimal mrp;
    private BigDecimal discountPercentage;
    private boolean inStock; // true if ANY active variant has stock
}
