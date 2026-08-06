package com.ladiesapparel.serviceability;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "serviceable_pincodes")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ServiceablePincode {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 10)
    private String pincode;

    @Column(length = 60)
    private String city;

    @Column(length = 60)
    private String state;

    @Column(nullable = false)
    @Builder.Default
    private boolean codAvailable = true;

    @Column(nullable = false)
    @Builder.Default
    private int estimatedDeliveryDays = 5;

    @Column(nullable = false)
    @Builder.Default
    private boolean active = true;
}
