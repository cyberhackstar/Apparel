package com.ladiesapparel.payment;

import com.ladiesapparel.common.ApiException;
import com.razorpay.RazorpayClient;
import com.razorpay.RazorpayException;
import com.razorpay.Utils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class RazorpayService {

    private final RazorpayClient razorpayClient;

    @Value("${razorpay.key-id}")
    private String keyId;

    @Value("${razorpay.key-secret}")
    private String keySecret;

    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;

    public String getKeyId() {
        return keyId;
    }

    /** Creates a Razorpay Order and returns its id. Amount is converted to paise (smallest unit). */
    public String createOrder(BigDecimal amount, String receipt) {
        try {
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            JSONObject orderRequest = new JSONObject();
            orderRequest.put("amount", amountInPaise);
            orderRequest.put("currency", "INR");
            orderRequest.put("receipt", receipt);
            orderRequest.put("payment_capture", 1); // auto-capture on success

            com.razorpay.Order razorpayOrder = razorpayClient.orders.create(orderRequest);
            return razorpayOrder.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay order creation failed: {}", e.getMessage());
            throw ApiException.badRequest("Unable to initiate payment. Please try again.");
        }
    }

    /** Verifies the signature returned by Razorpay Checkout.js after a successful payment. */
    public boolean verifyPaymentSignature(String razorpayOrderId, String razorpayPaymentId, String razorpaySignature) {
        try {
            JSONObject attributes = new JSONObject();
            attributes.put("razorpay_order_id", razorpayOrderId);
            attributes.put("razorpay_payment_id", razorpayPaymentId);
            attributes.put("razorpay_signature", razorpaySignature);

            return Utils.verifyPaymentSignature(attributes, keySecret);
        } catch (RazorpayException e) {
            log.error("Payment signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** Verifies the X-Razorpay-Signature header on incoming webhook calls. */
    public boolean verifyWebhookSignature(String payload, String signatureHeader) {
        try {
            return Utils.verifyWebhookSignature(payload, signatureHeader, webhookSecret);
        } catch (RazorpayException e) {
            log.error("Webhook signature verification failed: {}", e.getMessage());
            return false;
        }
    }

    /** Issues a full refund for a captured payment. Returns the refund id. */
    public String refundPayment(String razorpayPaymentId, BigDecimal amount) {
        try {
            long amountInPaise = amount.multiply(BigDecimal.valueOf(100)).longValueExact();

            JSONObject refundRequest = new JSONObject();
            refundRequest.put("amount", amountInPaise);

            com.razorpay.Refund refund = razorpayClient.payments.refund(razorpayPaymentId, refundRequest);
            return refund.get("id");
        } catch (RazorpayException e) {
            log.error("Razorpay refund failed: {}", e.getMessage());
            throw ApiException.badRequest("Refund could not be processed. Please try again or contact support.");
        }
    }
}
