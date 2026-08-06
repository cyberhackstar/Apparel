package com.ladiesapparel.order.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderItemResponse {
    private String productName;
    private String imageUrl;
    private String size;
    private String color;
    private String sku;
    private BigDecimal unitPrice;
    private int quantity;
    private BigDecimal lineTotal;
}
