package com.ladiesapparel.product.dto;

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
public class ProductResponse {
    private Long id;
    private String name;
    private String slug;
    private String description;
    private Long categoryId;
    private String categoryName;
    private String brand;
    private String fabric;
    private BigDecimal basePrice;
    private BigDecimal mrp;
    private BigDecimal discountPercentage;
    private BigDecimal gstPercentage;
    private List<String> tags;
    private Double averageRating;
    private Integer ratingCount;
    private boolean active;
    private List<ProductVariantResponse> variants;
    private List<ProductImageResponse> images;
}
