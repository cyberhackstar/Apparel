package com.ladiesapparel.serviceability;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ServiceablePincodeRepository extends JpaRepository<ServiceablePincode, Long> {
    Optional<ServiceablePincode> findByPincodeAndActiveTrue(String pincode);
    boolean existsByPincode(String pincode);
}
