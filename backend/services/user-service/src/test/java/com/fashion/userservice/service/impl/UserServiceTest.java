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
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private AddressRepository addressRepository;
    @Mock private RefreshTokenRepository refreshTokenRepository;
    @Mock private PasswordEncoder passwordEncoder;
    // @Mock private AuthenticationManager authenticationManager;

    @InjectMocks
    private UserService userService;

    @Test
    @DisplayName("Lấy Profile - Thành công")
    void getProfile_Success() {
        User user = User.builder().id("user-1").email("test@gmail.com").fullName("Fashion User").role(User.Role.CUSTOMER).build();
        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));

        UserProfileResponse response = userService.getProfile("user-1");

        assertNotNull(response);
        assertEquals("Fashion User", response.getFirstName());
        verify(userRepository).findById("user-1");
    }

    @Test
    @DisplayName("Đổi mật khẩu - Thất bại khi mật khẩu hiện tại sai")
    void changePassword_Fail_WrongCurrentPassword() {
        User user = User.builder().id("user-1").password("encoded-old-pass").build();
        ChangePasswordRequest request = new ChangePasswordRequest("wrong-pass", "new-pass");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("wrong-pass", "encoded-old-pass")).thenReturn(false);

        assertThrows(IllegalArgumentException.class, () -> userService.changePassword("user-1", request));
    }

    @Test
    @DisplayName("Đổi mật khẩu - Thành công và thu hồi tất cả Token")
    void changePassword_Success() {
        User user = User.builder().id("user-1").password("encoded-old-pass").build();
        ChangePasswordRequest request = new ChangePasswordRequest("old-pass", "new-pass");

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(passwordEncoder.matches("old-pass", "encoded-old-pass")).thenReturn(true);
        when(passwordEncoder.encode("new-pass")).thenReturn("encoded-new-pass");

        userService.changePassword("user-1", request);

        verify(userRepository).save(any(User.class));
        verify(refreshTokenRepository).revokeAllByUserId("user-1"); // Quan trọng: đảm bảo logout hết các thiết bị
    }

    @Test
    @DisplayName("Thêm địa chỉ - Tự động set mặc định cho địa chỉ đầu tiên")
    void addAddress_FirstTime_ShouldBeDefault() {
        User user = User.builder().id("user-1").build();
        AddressRequest request = new AddressRequest("Home", "0123", "Street", "Ward", "City", false);

        when(userRepository.findById("user-1")).thenReturn(Optional.of(user));
        when(addressRepository.countByUserId("user-1")).thenReturn(0);

        AddressResponse response = userService.addAddress("user-1", request);

        assertTrue(response.isDefault());
        verify(addressRepository).save(argThat(Address::isDefault));
    }

    @Test
    @DisplayName("Cập nhật địa chỉ - Xử lý chuyển đổi địa chỉ mặc định")
    void updateAddress_ChangeToDefault() {
        User user = User.builder().id("user-1").build();
        Address existingAddr = Address.builder().id("addr-1").user(user).isDefault(false).build();
        Address oldDefault = Address.builder().id("addr-old").user(user).isDefault(true).build();
        
        AddressRequest request = new AddressRequest("Work", "099", "Street", "Ward", "City", true);

        when(addressRepository.findById("addr-1")).thenReturn(Optional.of(existingAddr));
        when(addressRepository.findByUserIdAndIsDefaultTrue("user-1")).thenReturn(Optional.of(oldDefault));

        userService.updateAddress("user-1", "addr-1", request);

        assertFalse(oldDefault.isDefault()); // Địa chỉ cũ bị gỡ default
        verify(addressRepository).save(oldDefault);
        verify(addressRepository).save(argThat(a -> a.getId().equals("addr-1") && a.isDefault()));
    }

    @Test
    @DisplayName("Xóa địa chỉ - Ném lỗi nếu không tìm thấy hoặc sai chủ sở hữu")
    void deleteAddress_NotFound_ShouldThrowException() {
        when(addressRepository.findById("any-id")).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> userService.deleteAddress("user-1", "any-id"));
    }
}