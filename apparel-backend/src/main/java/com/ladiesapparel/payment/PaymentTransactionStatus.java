package com.ladiesapparel.payment;

/** Tracks the lifecycle of a single Razorpay payment attempt (distinct from Order.paymentStatus). */
public enum PaymentTransactionStatus {
    CREATED,
    SUCCESS,
    FAILED,
    REFUNDED
}
