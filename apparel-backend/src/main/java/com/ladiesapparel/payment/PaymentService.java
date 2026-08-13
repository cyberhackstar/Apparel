package com.ladiesapparel.payment;

import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import com.ladiesapparel.order.Order;
import com.ladiesapparel.order.OrderRepository;
import com.ladiesapparel.order.OrderStatus;
import com.ladiesapparel.order.PaymentStatus;
import com.ladiesapparel.payment.dto.CreateRazorpayOrderResponse;
import com.ladiesapparel.payment.dto.VerifyPaymentRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.json.JSONObject;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

@Service
@RequiredArgsConstructor
@Slf4j
public class PaymentService {

    private final PaymentRepository paymentRepository;
    private final OrderRepository orderRepository;
    private final RazorpayService razorpayService;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public CreateRazorpayOrderResponse createRazorpayOrder(String orderNumber) {
        User user = authenticatedUserProvider.getCurrentUser();
        Order order = orderRepository.findByOrderNumberAndUserId(orderNumber, user.getId())
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (order.getPaymentStatus() == PaymentStatus.PAID) {
            throw ApiException.badRequest("This order has already been paid for");
        }

        String razorpayOrderId = razorpayService.createOrder(order.getGrandTotal(), order.getOrderNumber());

        Payment payment = Payment.builder()
                .order(order)
                .razorpayOrderId(razorpayOrderId)
                .amount(order.getGrandTotal())
                .currency("INR")
                .status(PaymentTransactionStatus.CREATED)
                .build();
        paymentRepository.save(payment);

        return CreateRazorpayOrderResponse.builder()
                .razorpayOrderId(razorpayOrderId)
                .amountInPaise(order.getGrandTotal().multiply(BigDecimal.valueOf(100)).longValueExact())
                .currency("INR")
                .keyId(razorpayService.getKeyId())
                .orderNumber(order.getOrderNumber())
                .build();
    }

    @Transactional
    public void verifyPayment(VerifyPaymentRequest request) {
        Payment payment = paymentRepository.findByRazorpayOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> ApiException.notFound("Payment record not found"));

        boolean valid = razorpayService.verifyPaymentSignature(
                request.getRazorpayOrderId(), request.getRazorpayPaymentId(), request.getRazorpaySignature());

        if (!valid) {
            payment.setStatus(PaymentTransactionStatus.FAILED);
            payment.setFailureReason("Signature verification failed");
            paymentRepository.save(payment);
            throw ApiException.badRequest("Payment verification failed. If money was deducted, it will be refunded automatically.");
        }

        markPaymentSuccessful(payment, request.getRazorpayPaymentId());
    }

    /**
     * Handles Razorpay webhook events — the authoritative fallback in case the client
     * never calls /verify (e.g. browser closed right after payment).
     */
    @Transactional
    public void handleWebhook(String payload, String signatureHeader) {
        if (!razorpayService.verifyWebhookSignature(payload, signatureHeader)) {
            throw ApiException.unauthorized("Invalid webhook signature");
        }

        JSONObject event = new JSONObject(payload);
        String eventType = event.optString("event");

        JSONObject paymentEntity = event.optJSONObject("payload") != null
                ? event.getJSONObject("payload").optJSONObject("payment") != null
                        ? event.getJSONObject("payload").getJSONObject("payment").optJSONObject("entity")
                        : null
                : null;

        if (paymentEntity == null) {
            log.warn("Webhook event {} had no payment entity — ignoring", eventType);
            return;
        }

        String razorpayOrderId = paymentEntity.optString("order_id");
        String razorpayPaymentId = paymentEntity.optString("id");

        Payment payment = paymentRepository.findByRazorpayOrderId(razorpayOrderId).orElse(null);
        if (payment == null) {
            log.warn("Webhook referenced unknown razorpayOrderId {}", razorpayOrderId);
            return;
        }

        switch (eventType) {
            case "payment.captured" -> {
                if (payment.getStatus() != PaymentTransactionStatus.SUCCESS) {
                    markPaymentSuccessful(payment, razorpayPaymentId);
                }
            }
            case "payment.failed" -> {
                payment.setStatus(PaymentTransactionStatus.FAILED);
                payment.setFailureReason(paymentEntity.optString("error_description", "Payment failed"));
                paymentRepository.save(payment);
            }
            default -> log.info("Unhandled webhook event type: {}", eventType);
        }
    }

    @Transactional
    public void refundOrder(String orderNumber) {
        Order order = orderRepository.findByOrderNumber(orderNumber)
                .orElseThrow(() -> ApiException.notFound("Order not found"));

        if (order.getPaymentStatus() != PaymentStatus.PAID) {
            throw ApiException.badRequest("Only paid orders can be refunded");
        }

        Payment payment = paymentRepository.findTopByOrderOrderNumberOrderByCreatedAtDesc(orderNumber)
                .orElseThrow(() -> ApiException.notFound("No payment record found for this order"));

        String refundId = razorpayService.refundPayment(payment.getRazorpayPaymentId(), payment.getAmount());

        payment.setStatus(PaymentTransactionStatus.REFUNDED);
        payment.setRefundId(refundId);
        paymentRepository.save(payment);

        order.setPaymentStatus(PaymentStatus.REFUNDED);
        orderRepository.save(order);
    }

    private void markPaymentSuccessful(Payment payment, String razorpayPaymentId) {
        payment.setRazorpayPaymentId(razorpayPaymentId);
        payment.setStatus(PaymentTransactionStatus.SUCCESS);
        paymentRepository.save(payment);

        Order order = payment.getOrder();
        order.setPaymentStatus(PaymentStatus.PAID);
        if (order.getStatus() == OrderStatus.PLACED) {
            order.setStatus(OrderStatus.CONFIRMED);
        }
        orderRepository.save(order);
    }
}
