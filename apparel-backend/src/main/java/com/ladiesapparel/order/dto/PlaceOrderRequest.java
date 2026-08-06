package com.ladiesapparel.order.dto;

import com.ladiesapparel.order.PaymentMethod;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PlaceOrderRequest {

    @NotNull(message = "Delivery address is required")
    private Long addressId;

    // optional
    private String couponCode;

    @NotNull(message = "Payment method is required")
    private PaymentMethod paymentMethod;
}
