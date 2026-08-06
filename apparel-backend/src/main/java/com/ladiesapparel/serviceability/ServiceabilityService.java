package com.ladiesapparel.serviceability;

import com.ladiesapparel.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ServiceabilityService {

    private final ServiceablePincodeRepository repository;

    /**
     * If the admin hasn't configured any serviceable pincodes yet, every pincode is
     * treated as serviceable by default (COD-eligible, 5-day estimate) — so checkout
     * never breaks before the admin has had a chance to set up their delivery zones.
     */
    public ServiceabilityResponse check(String pincode) {
        if (repository.count() == 0) {
            return ServiceabilityResponse.builder()
                    .pincode(pincode)
                    .serviceable(true)
                    .codAvailable(true)
                    .estimatedDeliveryDays(5)
                    .build();
        }

        return repository.findByPincodeAndActiveTrue(pincode)
                .map(p -> ServiceabilityResponse.builder()
                        .pincode(p.getPincode())
                        .serviceable(true)
                        .codAvailable(p.isCodAvailable())
                        .estimatedDeliveryDays(p.getEstimatedDeliveryDays())
                        .city(p.getCity())
                        .state(p.getState())
                        .build())
                .orElseGet(() -> ServiceabilityResponse.builder()
                        .pincode(pincode)
                        .serviceable(false)
                        .codAvailable(false)
                        .build());
    }

    @Transactional
    public void addPincode(PincodeRequest request) {
        if (repository.existsByPincode(request.getPincode())) {
            throw ApiException.conflict("Pincode already exists");
        }

        ServiceablePincode entity = ServiceablePincode.builder()
                .pincode(request.getPincode())
                .city(request.getCity())
                .state(request.getState())
                .codAvailable(request.isCodAvailable())
                .estimatedDeliveryDays(request.getEstimatedDeliveryDays())
                .active(true)
                .build();

        repository.save(entity);
    }

    @Transactional
    public void removePincode(Long id) {
        ServiceablePincode entity = repository.findById(id)
                .orElseThrow(() -> ApiException.notFound("Pincode not found"));
        repository.delete(entity);
    }

    public List<ServiceablePincode> getAll() {
        return repository.findAll().stream().collect(Collectors.toList());
    }
}
