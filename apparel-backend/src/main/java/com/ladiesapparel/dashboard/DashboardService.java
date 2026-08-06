package com.ladiesapparel.dashboard;

import com.ladiesapparel.auth.Role;
import com.ladiesapparel.auth.UserRepository;
import com.ladiesapparel.dashboard.dto.DailySalesResponse;
import com.ladiesapparel.dashboard.dto.DashboardSummaryResponse;
import com.ladiesapparel.dashboard.dto.LowStockVariantResponse;
import com.ladiesapparel.dashboard.dto.TopProductResponse;
import com.ladiesapparel.order.Order;
import com.ladiesapparel.order.OrderItemRepository;
import com.ladiesapparel.order.OrderRepository;
import com.ladiesapparel.order.OrderStatus;
import com.ladiesapparel.product.ProductVariant;
import com.ladiesapparel.product.ProductVariantRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final OrderRepository orderRepository;
    private final OrderItemRepository orderItemRepository;
    private final ProductVariantRepository variantRepository;
    private final UserRepository userRepository;

    private static final ZoneId IST = ZoneId.of("Asia/Kolkata");

    @Cacheable(cacheNames = "dashboardSummary")
    public DashboardSummaryResponse getSummary() {
        Instant startOfToday = LocalDate.now(IST).atStartOfDay(IST).toInstant();

        return DashboardSummaryResponse.builder()
                .totalRevenue(orderRepository.getTotalRevenue())
                .totalOrders(orderRepository.count())
                .totalCustomers(userRepository.countByRole(Role.CUSTOMER))
                .todayRevenue(orderRepository.getRevenueSince(startOfToday))
                .todayOrders(orderRepository.countByCreatedAtAfter(startOfToday))
                .pendingOrders(orderRepository.countByStatus(OrderStatus.PLACED)
                        + orderRepository.countByStatus(OrderStatus.CONFIRMED))
                .cancelledOrders(orderRepository.countByStatus(OrderStatus.CANCELLED))
                .build();
    }

    @Cacheable(cacheNames = "topProducts", key = "#limit")
    public List<TopProductResponse> getTopSellingProducts(int limit) {
        Pageable pageable = PageRequest.of(0, limit);
        return orderItemRepository.findTopSellingProducts(pageable).stream()
                .map(p -> TopProductResponse.builder()
                        .productId(p.getProductId())
                        .productName(p.getProductName())
                        .totalSold(p.getTotalSold())
                        .build())
                .collect(Collectors.toList());
    }

    @Cacheable(cacheNames = "lowStock", key = "#threshold")
    public List<LowStockVariantResponse> getLowStockVariants(int threshold) {
        List<ProductVariant> variants = variantRepository
                .findByStockQuantityLessThanAndActiveTrueOrderByStockQuantityAsc(threshold);

        return variants.stream()
                .map(v -> LowStockVariantResponse.builder()
                        .variantId(v.getId())
                        .productName(v.getProduct().getName())
                        .size(v.getSize())
                        .color(v.getColor())
                        .sku(v.getSku())
                        .stockQuantity(v.getStockQuantity())
                        .build())
                .collect(Collectors.toList());
    }

    public List<DailySalesResponse> getDailySalesReport(Instant from, Instant to) {
        List<Object[]> rows = orderRepository.getDailySalesReport(from, to);
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

        return rows.stream()
                .map(row -> {
                    java.sql.Timestamp day = (java.sql.Timestamp) row[0];
                    BigDecimal revenue = (BigDecimal) row[1];
                    long ordersCount = ((Number) row[2]).longValue();

                    String dateStr = day.toInstant().atZone(IST).format(formatter);

                    return DailySalesResponse.builder()
                            .date(dateStr)
                            .revenue(revenue)
                            .ordersCount(ordersCount)
                            .build();
                })
                .collect(Collectors.toList());
    }

    /** Simple CSV export of all orders in a date range — for accounting/GST filing. */
    public String exportOrdersCsv(Instant from, Instant to) {
        List<Order> orders = orderRepository.findAllBetween(from, to);

        StringBuilder csv = new StringBuilder();
        csv.append("Order Number,Date,Customer Email,Status,Payment Method,Payment Status,")
           .append("Subtotal,Discount,Shipping,GST,Grand Total\n");

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

        for (Order order : orders) {
            csv.append(escapeCsv(order.getOrderNumber())).append(',')
               .append(order.getCreatedAt().atZone(IST).format(formatter)).append(',')
               .append(escapeCsv(order.getUser().getEmail())).append(',')
               .append(order.getStatus()).append(',')
               .append(order.getPaymentMethod()).append(',')
               .append(order.getPaymentStatus()).append(',')
               .append(order.getSubtotal()).append(',')
               .append(order.getDiscountAmount()).append(',')
               .append(order.getShippingCharge()).append(',')
               .append(order.getGstAmount()).append(',')
               .append(order.getGrandTotal()).append('\n');
        }

        return csv.toString();
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
