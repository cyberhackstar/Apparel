package com.ladiesapparel.payment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CreateRazorpayOrderResponse {
    private String razorpayOrderId;
    private long amountInPaise;
    private String currency;
    private String keyId;       // public key — safe to expose to frontend for Checkout.js
    private String orderNumber;
}
