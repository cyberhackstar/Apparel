package com.ladiesapparel.cart.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class AddToCartRequest {

    @NotNull(message = "Variant is required")
    private Long variantId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private int quantity = 1;
}
