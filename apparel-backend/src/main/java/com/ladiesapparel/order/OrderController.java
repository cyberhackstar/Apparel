package com.ladiesapparel.order;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.common.PagedResponse;
import com.ladiesapparel.order.dto.OrderResponse;
import com.ladiesapparel.order.dto.OrderStatusUpdateRequest;
import com.ladiesapparel.order.dto.PlaceOrderRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    // ---------- Customer endpoints ----------

    @PostMapping("/api/orders")
    public ResponseEntity<ApiResponse<OrderResponse>> placeOrder(@Valid @RequestBody PlaceOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order placed successfully", orderService.placeOrder(request)));
    }

    @GetMapping("/api/orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getMyOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", orderService.getMyOrders(pageable)));
    }

    @GetMapping("/api/orders/{orderNumber}")
    public ResponseEntity<ApiResponse<OrderResponse>> getMyOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("Order fetched", orderService.getMyOrder(orderNumber)));
    }

    @PostMapping("/api/orders/{orderNumber}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(@PathVariable String orderNumber) {
        return ResponseEntity.ok(ApiResponse.success("Order cancelled", orderService.cancelOrder(orderNumber)));
    }

    // ---------- Admin endpoints ----------

    @GetMapping("/api/admin/orders")
    public ResponseEntity<ApiResponse<PagedResponse<OrderResponse>>> getAllOrders(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int pageSize) {

        Pageable pageable = PageRequest.of(page, pageSize, Sort.by("createdAt").descending());
        return ResponseEntity.ok(ApiResponse.success("Orders fetched", orderService.getAllOrdersForAdmin(pageable)));
    }

    @PatchMapping("/api/admin/orders/{orderNumber}/status")
    public ResponseEntity<ApiResponse<OrderResponse>> updateStatus(@PathVariable String orderNumber,
                                                                    @Valid @RequestBody OrderStatusUpdateRequest request) {
        return ResponseEntity.ok(ApiResponse.success("Order status updated",
                orderService.updateStatus(orderNumber, request.getStatus())));
    }
}
