package com.ladiesapparel.serviceability;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PincodeRequest {

    @NotBlank
    @Pattern(regexp = "^\\d{6}$", message = "Pincode must be 6 digits")
    private String pincode;

    private String city;

    private String state;

    private boolean codAvailable = true;

    private int estimatedDeliveryDays = 5;
}
