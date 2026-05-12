package com.fashion.userservice.service;

import com.fashion.userservice.dto.request.AddressRequest;
import com.fashion.userservice.dto.request.ChangePasswordRequest;
import com.fashion.userservice.dto.request.UpdateProfileRequest;
import com.fashion.userservice.dto.response.AddressResponse;
import com.fashion.userservice.dto.response.UserProfileResponse;
import com.fashion.userservice.entity.Address;
import com.fashion.userservice.entity.User;
import com.fashion.userservice.exception.ResourceNotFoundException;
import com.fashion.userservice.repository.AddressRepository;
import com.fashion.userservice.repository.RefreshTokenRepository;
import com.fashion.userservice.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final AddressRepository addressRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;

    public UserProfileResponse getProfile(String userId) {
        User user = findUserById(userId);
        return toProfileResponse(user);
    }

    @Transactional
    public UserProfileResponse updateProfile(String userId, UpdateProfileRequest request) {
        User user = findUserById(userId);
        user.setFullName(request.resolveFullName());
        user.setPhone(request.resolvePhone());
        if (request.getGender() != null) {
            user.setGender(request.getGender());
        }
        userRepository.save(user);
        return toProfileResponse(user);
    }

    @Transactional
    public void changePassword(String userId, ChangePasswordRequest request) {
        User user = findUserById(userId);

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPassword())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }
        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        // Password rotation invalidates all existing sessions.
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ─── Address ───────────────────────────────────────────────────────────────

    public List<AddressResponse> getAddresses(String userId) {
        return addressRepository.findByUserId(userId)
                .stream()
                .map(this::toAddressResponse)
                .toList();
    }

    @Transactional
    public AddressResponse addAddress(String userId, AddressRequest request) {
        User user = findUserById(userId);

        // If first address, set as default
        boolean isFirst = addressRepository.countByUserId(userId) == 0;
        if (request.isDefault() || isFirst) {
            clearDefaultAddresses(userId);
        }

        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .phoneNumber(request.getPhoneNumber())
                .street(request.getStreet())
                .ward(request.getWard())
                .city(request.getCity())
                .isDefault(request.isDefault() || isFirst)
                .build();

        addressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public AddressResponse updateAddress(String userId, String addressId, AddressRequest request) {
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));

        if (request.isDefault()) clearDefaultAddresses(userId);

        address.setFullName(request.getFullName());
        address.setPhoneNumber(request.getPhoneNumber());
        address.setStreet(request.getStreet());
        address.setWard(request.getWard());
        address.setCity(request.getCity());
        address.setDefault(request.isDefault());
        addressRepository.save(address);
        return toAddressResponse(address);
    }

    @Transactional
    public void deleteAddress(String userId, String addressId) {
        Address address = addressRepository.findById(addressId)
                .filter(a -> a.getUser().getId().equals(userId))
                .orElseThrow(() -> new ResourceNotFoundException("Address not found"));
        addressRepository.delete(address);
    }

    // ─── Private helpers ───────────────────────────────────────────────────────

    private User findUserById(String userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User not found: " + userId));
    }

    private void clearDefaultAddresses(String userId) {
        addressRepository.findByUserIdAndIsDefaultTrue(userId)
                .ifPresent(addr -> {
                    addr.setDefault(false);
                    addressRepository.save(addr);
                });
    }

    private UserProfileResponse toProfileResponse(User user) {
        String[] nameParts = splitFullName(user.getFullName());
        return UserProfileResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .firstName(nameParts[1])
                .lastName(nameParts[0])
                .phoneNumber(user.getPhone())
                .avatarUrl(user.getAvatar())
                .role(user.getRole().name())
                .gender(user.getGender())
                .build();
    }

    /**
     * The database stores a single full_name column, but the storefront profile
     * screen edits first and last name separately.
     */
    private String[] splitFullName(String fullName) {
        if (fullName == null || fullName.isBlank()) {
            return new String[]{"", ""};
        }

        String normalizedName = fullName.trim().replaceAll("\\s+", " ");
        int lastSpaceIndex = normalizedName.lastIndexOf(' ');
        if (lastSpaceIndex < 0) {
            return new String[]{"", normalizedName};
        }

        return new String[]{
                normalizedName.substring(0, lastSpaceIndex),
                normalizedName.substring(lastSpaceIndex + 1)
        };
    }

    private AddressResponse toAddressResponse(Address address) {
        return AddressResponse.builder()
                .id(address.getId())
                .fullName(address.getFullName())
                .phoneNumber(address.getPhoneNumber())
                .street(address.getStreet())
                .ward(address.getWard())
                .city(address.getCity())
                .isDefault(address.isDefault())
                .build();
    }
}
