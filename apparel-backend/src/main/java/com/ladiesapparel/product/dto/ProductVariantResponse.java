package com.ladiesapparel.product.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProductVariantResponse {
    private Long id;
    private String size;
    private String color;
    private String sku;
    private Integer stockQuantity;
    private BigDecimal additionalPrice;
    private BigDecimal finalPrice; // basePrice + additionalPrice, computed
    private boolean active;
    private boolean inStock;
}
