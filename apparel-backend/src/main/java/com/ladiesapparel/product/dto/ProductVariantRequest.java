package com.ladiesapparel.product.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ProductVariantRequest {

    @NotBlank(message = "Size is required")
    private String size;

    @NotBlank(message = "Color is required")
    private String color;

    @NotBlank(message = "SKU is required")
    private String sku;

    @Min(value = 0, message = "Stock cannot be negative")
    private Integer stockQuantity;

    @DecimalMin(value = "0.0", message = "Additional price cannot be negative")
    private BigDecimal additionalPrice;
}
