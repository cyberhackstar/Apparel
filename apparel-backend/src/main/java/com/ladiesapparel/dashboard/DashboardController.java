package com.ladiesapparel.dashboard;

import com.ladiesapparel.common.ApiResponse;
import com.ladiesapparel.dashboard.dto.DailySalesResponse;
import com.ladiesapparel.dashboard.dto.DashboardSummaryResponse;
import com.ladiesapparel.dashboard.dto.LowStockVariantResponse;
import com.ladiesapparel.dashboard.dto.TopProductResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DashboardController {

    private final DashboardService dashboardService;

    @GetMapping("/dashboard/summary")
    public ResponseEntity<ApiResponse<DashboardSummaryResponse>> getSummary() {
        return ResponseEntity.ok(ApiResponse.success("Summary fetched", dashboardService.getSummary()));
    }

    @GetMapping("/dashboard/top-products")
    public ResponseEntity<ApiResponse<List<TopProductResponse>>> getTopProducts(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(ApiResponse.success("Top products fetched", dashboardService.getTopSellingProducts(limit)));
    }

    @GetMapping("/dashboard/low-stock")
    public ResponseEntity<ApiResponse<List<LowStockVariantResponse>>> getLowStock(
            @RequestParam(defaultValue = "5") int threshold) {
        return ResponseEntity.ok(ApiResponse.success("Low stock items fetched", dashboardService.getLowStockVariants(threshold)));
    }

    @GetMapping("/reports/sales")
    public ResponseEntity<ApiResponse<List<DailySalesResponse>>> getSalesReport(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {
        return ResponseEntity.ok(ApiResponse.success("Sales report fetched", dashboardService.getDailySalesReport(from, to)));
    }

    @GetMapping("/reports/orders/export")
    public ResponseEntity<String> exportOrders(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) Instant to) {

        String csv = dashboardService.exportOrdersCsv(from, to);

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=orders.csv")
                .header(HttpHeaders.CONTENT_TYPE, "text/csv")
                .body(csv);
    }
}
