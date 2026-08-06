package com.ladiesapparel.address;

import com.ladiesapparel.address.dto.AddressRequest;
import com.ladiesapparel.address.dto.AddressResponse;
import com.ladiesapparel.auth.AuthenticatedUserProvider;
import com.ladiesapparel.auth.User;
import com.ladiesapparel.common.ApiException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final AuthenticatedUserProvider authenticatedUserProvider;

    @Transactional
    public AddressResponse addAddress(AddressRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();

        boolean isFirstAddress = addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId()).isEmpty();
        boolean makeDefault = request.isDefault() || isFirstAddress;

        if (makeDefault) {
            clearExistingDefault(user.getId());
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phone(request.getPhone())
                .addressLine1(request.getAddressLine1())
                .addressLine2(request.getAddressLine2())
                .landmark(request.getLandmark())
                .city(request.getCity())
                .state(request.getState())
                .pincode(request.getPincode())
                .addressType(request.getAddressType() != null ? request.getAddressType() : AddressType.HOME)
                .isDefault(makeDefault)
                .build();

        addressRepository.save(address);
        return toResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(Long addressId, AddressRequest request) {
        User user = authenticatedUserProvider.getCurrentUser();
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Address not found"));

        if (request.isDefault() && !address.isDefault()) {
            clearExistingDefault(user.getId());
            address.setDefault(true);
        }

        address.setFullName(request.getFullName());
        address.setPhone(request.getPhone());
        address.setAddressLine1(request.getAddressLine1());
        address.setAddressLine2(request.getAddressLine2());
        address.setLandmark(request.getLandmark());
        address.setCity(request.getCity());
        address.setState(request.getState());
        address.setPincode(request.getPincode());
        if (request.getAddressType() != null) {
            address.setAddressType(request.getAddressType());
        }

        addressRepository.save(address);
        return toResponse(address);
    }

    @Transactional
    public void deleteAddress(Long addressId) {
        User user = authenticatedUserProvider.getCurrentUser();
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Address not found"));

        boolean wasDefault = address.isDefault();
        addressRepository.delete(address);

        // promote the most recent remaining address to default, if one exists
        if (wasDefault) {
            addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                    .stream().findFirst()
                    .ifPresent(next -> {
                        next.setDefault(true);
                        addressRepository.save(next);
                    });
        }
    }

    @Transactional
    public AddressResponse setDefault(Long addressId) {
        User user = authenticatedUserProvider.getCurrentUser();
        Address address = addressRepository.findByIdAndUserId(addressId, user.getId())
                .orElseThrow(() -> ApiException.notFound("Address not found"));

        clearExistingDefault(user.getId());
        address.setDefault(true);
        addressRepository.save(address);
        return toResponse(address);
    }

    public List<AddressResponse> getMyAddresses() {
        User user = authenticatedUserProvider.getCurrentUser();
        return addressRepository.findByUserIdOrderByIsDefaultDescCreatedAtDesc(user.getId())
                .stream().map(this::toResponse).collect(Collectors.toList());
    }

    private void clearExistingDefault(Long userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId).ifPresent(existing -> {
            existing.setDefault(false);
            addressRepository.save(existing);
        });
    }

    private AddressResponse toResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phone(address.getPhone())
                .addressLine1(address.getAddressLine1())
                .addressLine2(address.getAddressLine2())
                .landmark(address.getLandmark())
                .city(address.getCity())
                .state(address.getState())
                .pincode(address.getPincode())
                .addressType(address.getAddressType())
                .isDefault(address.isDefault())
                .build();
    }
}
