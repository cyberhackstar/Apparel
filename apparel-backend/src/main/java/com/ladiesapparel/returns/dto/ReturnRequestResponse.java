package com.ladiesapparel.returns.dto;

import com.ladiesapparel.returns.ReturnStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReturnRequestResponse {
    private Long id;
    private String orderNumber;
    private String customerEmail;
    private String reason;
    private ReturnStatus status;
    private String adminNotes;
    private BigDecimal refundAmount;
    private Instant requestedAt;
    private Instant resolvedAt;
}
