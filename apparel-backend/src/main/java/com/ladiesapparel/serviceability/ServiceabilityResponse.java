package com.ladiesapparel.serviceability;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceabilityResponse {
    private String pincode;
    private boolean serviceable;
    private boolean codAvailable;
    private Integer estimatedDeliveryDays;
    private String city;
    private String state;
}
