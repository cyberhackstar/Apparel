package com.ladiesapparel.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class LowStockVariantResponse {
    private Long variantId;
    private String productName;
    private String size;
    private String color;
    private String sku;
    private int stockQuantity;
}
