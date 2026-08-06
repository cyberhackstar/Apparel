package com.ladiesapparel.dashboard.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardSummaryResponse {
    private BigDecimal totalRevenue;
    private long totalOrders;
    private long totalCustomers;
    private BigDecimal todayRevenue;
    private long todayOrders;
    private long pendingOrders;
    private long cancelledOrders;
}
