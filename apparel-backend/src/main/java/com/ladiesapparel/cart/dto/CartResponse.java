package com.ladiesapparel.cart.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private List<CartItemResponse> items;
    private int totalItems;
    private BigDecimal subtotal;      // sum of unitPrice * qty
    private BigDecimal totalMrp;      // sum of mrp * qty
    private BigDecimal totalDiscount; // totalMrp - subtotal
    private boolean hasOutOfStockItems;
}
