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
public class DailySalesResponse {
    private String date;       // yyyy-MM-dd
    private BigDecimal revenue;
    private long ordersCount;
}
