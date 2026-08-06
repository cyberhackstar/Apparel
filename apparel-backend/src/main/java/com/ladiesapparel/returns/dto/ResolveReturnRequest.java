package com.ladiesapparel.returns.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
public class ResolveReturnRequest {

    @Size(max = 500)
    private String adminNotes;

    // required only when completing (issuing the refund)
    @DecimalMin(value = "0.0")
    private BigDecimal refundAmount;
}
