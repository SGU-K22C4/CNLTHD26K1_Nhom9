package com.fashion.userservice.service;

import com.fashion.userservice.dto.request.AddressRequest;
import com.fashion.userservice.dto.request.ChangePasswordRequest;
import com.fashion.userservice.dto.response.AddressResponse;
import com.fashion.userservice.dto.response.UserProfileResponse;
import com.fashion.userservice.entity.Address;
import com.fashion.userservice.entity.User;
import com.fashion.userservice.exception.ResourceNotFoundException;
import com.fashion.userservice.repository.AddressRepository;
import com.fashion.userservice.repository.RefreshTokenRepository;
import com.fashion.userservice.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private AddressRepository addressRepository;

    @Mock
    private RefreshTokenRepository refreshTokenRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserService userService;

    @Test
    void should_ReturnUserProfile_When_UserExists() {
        User user = User.builder()
                .id("user-1")
                .email("test@gmail.com")
                .fullName("Fashion User")
                .role(User.Role.CUSTOMER)
                .build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile("user-1");

        assertEquals("Fashion User", response.getFirstName());
        verify(userRepository).findById("user-1");
    }

    @Test
    void should_ThrowIllegalArgumentException_When_CurrentPasswordIsIncorrect() {
        User user = User.builder().id("user-1").password("encoded-old-pass").build();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("wrong-pass");
        request.setNewPassword("new-pass");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "encoded-old-pass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword("user-1", request));
    }

    @Test
    void should_SaveNewPasswordAndRevokeTokens_When_ChangePasswordSucceeds() {
        User user = User.builder().id("user-1").password("encoded-old-pass").build();
        ChangePasswordRequest request = new ChangePasswordRequest();
        request.setCurrentPassword("old-pass");
        request.setNewPassword("new-pass");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "encoded-old-pass")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new-pass");

        userService.changePassword("user-1", request);

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).revokeAllByUserId("user-1");
    }

    @Test
    void should_SetFirstAddressAsDefault_When_UserHasNoAddresses() {
        User user = User.builder().id("user-1").build();
        AddressRequest request = new AddressRequest();
        request.setFullName("Home");
        request.setPhoneNumber("0123");
        request.setStreet("Street");
        request.setWard("Ward");
        request.setCity("City");
        request.setDefault(false);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId("user-1")).thenReturn(0);

        AddressResponse response = userService.addAddress("user-1", request);

        assertTrue(response.isDefault());
        verify(addressRepository).save(any(Address.class));
    }

    @Test
    void should_ClearOldDefaultAddress_When_UpdatingAddressToDefault() {
        User user = User.builder().id("user-1").build();
        Address existingAddress = Address.builder().id("addr-1").user(user).isDefault(false).build();
        Address oldDefaultAddress = Address.builder().id("addr-old").user(user).isDefault(true).build();
        AddressRequest request = new AddressRequest();
        request.setFullName("Work");
        request.setPhoneNumber("099");
        request.setStreet("Street");
        request.setWard("Ward");
        request.setCity("City");
        request.setDefault(true);

        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(existingAddress));
        when(addressRepository.findByUserIdAndIsDefaultTrue("user-1")).thenReturn(Optional.of(oldDefaultAddress));

        userService.updateAddress("user-1", "addr-1", request);

        assertFalse(oldDefaultAddress.isDefault());
        verify(addressRepository).save(oldDefaultAddress);
        verify(addressRepository).save(existingAddress);
    }

    @Test
    void should_ThrowResourceNotFoundException_When_AddressDoesNotExist() {
        when(addressRepository.findById("any-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteAddress("user-1", "any-id"));
    }
}
