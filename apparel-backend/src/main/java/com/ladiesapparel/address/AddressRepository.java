package com.ladiesapparel.address;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface AddressRepository extends JpaRepository<Address, Long> {
    List<Address> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);
    Optional<Address> findByUserIdAndIsDefaultTrue(Long userId);
    Optional<Address> findByIdAndUserId(Long id, Long userId);
}
