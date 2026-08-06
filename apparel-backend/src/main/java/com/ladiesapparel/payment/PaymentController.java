package com.ladiesapparel.payment;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.payment.dto.CreateRazorpayOrderResponse;
import com.ladiesapparel.payment.dto.VerifyPaymentRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.BufferedReader;
import java.io.IOException;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/payments")
@RequiredArgsConstructor
public class PaymentController {

    private final PaymentService paymentService;

    @PostMapping("/razorpay/create-order/{orderNumber}")
    public ResponseEntity<ApiResponse<CreateRazorpayOrderResponse>> createOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("Razorpay order created",
                paymentService.createRazorpayOrder(orderNumber)));
    }

    @PostMapping("/razorpay/verify")
    public ResponseEntity<ApiResponse<Object>> verify(@Valid @RequestBody VerifyPaymentRequest request) {
        paymentService.verifyPayment(request);
        return ResponseEntity.ok(ApiResponse.success("Payment verified successfully"));
    }

    /**
     * Public webhook endpoint — must be added to /api/public/** (or explicitly permitAll)
     * in SecurityConfig since Razorpay calls this directly with no JWT.
     */
    @PostMapping("/razorpay/webhook")
    public ResponseEntity<String> webhook(HttpServletRequest request,
                                           @RequestHeader("X-Razorpay-Signature") String signature) throws IOException {
        String payload = readBody(request);
        paymentService.handleWebhook(payload, signature);
        return ResponseEntity.ok("OK");
    }

    private String readBody(HttpServletRequest request) throws IOException {
        try (BufferedReader reader = request.getReader()) {
            return reader.lines().collect(Collectors.joining(System.lineSeparator()));
        }
    }
}
