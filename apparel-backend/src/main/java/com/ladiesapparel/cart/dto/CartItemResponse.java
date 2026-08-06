package com.ladiesapparel.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartItemResponse {
    private Long cartItemId;
    private Long productId;
    private String productName;
    private String productSlug;
    private String imageUrl;
    private Long variantId;
    private String size;
    private String color;
    private BigDecimal unitPrice;
    private BigDecimal mrp;
    private int quantity;
    private BigDecimal lineTotal;
    private int availableStock;
    private boolean inStock; // variant active AND enough stock for the requested quantity
}
