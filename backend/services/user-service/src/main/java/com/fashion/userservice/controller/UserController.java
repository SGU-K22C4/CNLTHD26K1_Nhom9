package com.fashion.userservice.controller;

import com.fashion.userservice.dto.request.AddressRequest;
import com.fashion.userservice.dto.request.ChangePasswordRequest;
import com.fashion.userservice.dto.request.UpdateProfileRequest;
import com.fashion.userservice.dto.response.AddressResponse;
import com.fashion.userservice.dto.response.UserProfileResponse;
import com.fashion.userservice.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Header X-User-Id được inject bởi API Gateway sau khi xác thực JWT
    @GetMapping("/me")
    public ResponseEntity<UserProfileResponse> getProfile(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getProfile(userId));
    }

    @PutMapping("/me")
    public ResponseEntity<UserProfileResponse> updateProfile(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody UpdateProfileRequest request) {
        return ResponseEntity.ok(userService.updateProfile(userId, request));
    }

    @PatchMapping("/me/password")
    public ResponseEntity<Void> changePassword(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody ChangePasswordRequest request) {
        userService.changePassword(userId, request);
        return ResponseEntity.noContent().build();
    }

    // ─── Addresses ─────────────────────────────────────────────────────────────

    @GetMapping("/me/addresses")
    public ResponseEntity<List<AddressResponse>> getAddresses(@RequestHeader("X-User-Id") String userId) {
        return ResponseEntity.ok(userService.getAddresses(userId));
    }

    @PostMapping("/me/addresses")
    public ResponseEntity<AddressResponse> addAddress(
            @RequestHeader("X-User-Id") String userId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(userService.addAddress(userId, request));
    }

    @PutMapping("/me/addresses/{addressId}")
    public ResponseEntity<AddressResponse> updateAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String addressId,
            @Valid @RequestBody AddressRequest request) {
        return ResponseEntity.ok(userService.updateAddress(userId, addressId, request));
    }

    @DeleteMapping("/me/addresses/{addressId}")
    public ResponseEntity<Void> deleteAddress(
            @RequestHeader("X-User-Id") String userId,
            @PathVariable String addressId) {
        userService.deleteAddress(userId, addressId);
        return ResponseEntity.noContent().build();
    }
}
