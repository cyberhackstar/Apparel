package com.ladiesapparel.order.dto;

import com.ladiesapparel.order.OrderStatus;
import com.ladiesapparel.order.PaymentMethod;
import com.ladiesapparel.order.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {
    private String orderNumber;
    private OrderStatus status;
    private PaymentMethod paymentMethod;
    private PaymentStatus paymentStatus;

    private String recipientName;
    private String recipientPhone;
    private String addressLine1;
    private String addressLine2;
    private String landmark;
    private String city;
    private String state;
    private String pincode;

    private BigDecimal subtotal;
    private BigDecimal discountAmount;
    private String couponCode;
    private BigDecimal shippingCharge;
    private BigDecimal gstAmount;
    private BigDecimal grandTotal;

    private List<OrderItemResponse> items;
    private Instant createdAt;
}
