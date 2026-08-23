package com.mindq.admin.controller;

import com.mindq.admin.dto.AdminDashboardStats;
import com.mindq.admin.dto.AdminUserResponse;
import com.mindq.admin.dto.UpdateUserRoleRequest;
import com.mindq.repository.UserRepository;
import com.mindq.admin.dto.UpdateUserStatusRequest;
import com.mindq.admin.service.AdminService;
import com.mindq.common.api.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final UserRepository userRepository;

    @GetMapping("/dashboard")
    public ResponseEntity<ApiResponse<AdminDashboardStats>> getDashboard() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getDashboardStats(), "Admin dashboard stats"));
    }

    @GetMapping("/users")
    public ResponseEntity<ApiResponse<List<AdminUserResponse>>> getAllUsers() {
        return ResponseEntity.ok(ApiResponse.success(adminService.getAllUsers(), "Users retrieved"));
    }

    @PutMapping("/users/{userId}/status")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusRequest request,
            Authentication authentication) {
        com.mindq.model.User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        adminService.updateUserStatus(userId, request.getStatus(), admin.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "User status updated"), "Success"));
    }

    @DeleteMapping("/users/{userId}")
    public ResponseEntity<ApiResponse<Map<String, String>>> deleteUser(
            @PathVariable Long userId,
            Authentication authentication) {
        // Get admin ID from the authenticated user
        com.mindq.model.User admin = userRepository.findByEmail(authentication.getName())
                .orElseThrow(() -> new IllegalArgumentException("Admin not found"));
        adminService.deleteUser(userId, admin.getId());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "User deleted successfully"), "Success"));
    }

    @PutMapping("/users/{userId}/role")
    public ResponseEntity<ApiResponse<Map<String, String>>> updateUserRole(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserRoleRequest request) {
        adminService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success(Map.of("message", "User role updated"), "Success"));
    }
}
