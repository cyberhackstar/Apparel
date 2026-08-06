package com.ladiesapparel.payment;

import com.ladiesapparel.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/payments")
@RequiredArgsConstructor
public class AdminPaymentController {

    private final PaymentService paymentService;

    @PostMapping("/refund/{orderNumber}")
    public ResponseEntity<ApiResponse<Object>> refund(@PathVariable String orderNumber) {
        paymentService.refundOrder(orderNumber);
        return ResponseEntity.ok(ApiResponse.success("Refund initiated"));
    }
}
