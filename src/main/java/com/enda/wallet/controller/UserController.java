package com.enda.wallet.controller;

import com.enda.wallet.model.dto.request.RegisterRequest;
import com.enda.wallet.model.dto.response.ApiResponse;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.model.enums.Role;
import com.enda.wallet.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
@Slf4j
public class UserController {

    private final UserService userService;

    // ✅ Inscription publique conservée (sans restriction)
    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<User> createUser(@Valid @RequestBody RegisterRequest request) {
        log.info("📝 Inscription publique : {}", request.getUsername());
        User created = userService.createUser(request);
        created.setPasswordHash(null);
        return ApiResponse.success("Inscription réussie", created);
    }

    // ✅ Récupération du solde (pour Initiateur et Admin)
    @GetMapping("/balance")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    public ApiResponse<Double> getBalance(@AuthenticationPrincipal User user) {
        double balance = userService.getBalance(user.getUsername());
        return ApiResponse.success(balance);
    }

    // ─── Autres endpoints inchangés ───
    @PostMapping("/forgot-password")
    public ApiResponse<Void> forgotPassword(@RequestBody Map<String, String> request) {
        userService.sendPasswordResetEmail(request.get("email"));
        return ApiResponse.success("Email de réinitialisation envoyé", null);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(@RequestBody Map<String, String> request) {
        userService.resetPassword(request.get("token"), request.get("password"));
        return ApiResponse.success("Mot de passe réinitialisé", null);
    }

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<User> getCurrentUser(@AuthenticationPrincipal User user) {
        return ApiResponse.success(user);
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Page<User>> getAllUsers(@PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(userService.getAllUsers(pageable));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> getUserById(@PathVariable Long id) {
        return ApiResponse.success(userService.getUserById(id));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> updateUser(@PathVariable Long id, @RequestBody User user) {
        return ApiResponse.success("Utilisateur mis à jour", userService.updateUser(id, user));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<Void> deactivateUser(@PathVariable Long id) {
        userService.deactivateUser(id);
        return ApiResponse.success("Utilisateur désactivé", null);
    }

    @PatchMapping("/{id}/role")
    @PreAuthorize("hasRole('ADMIN')")
    public ApiResponse<User> changeUserRole(@PathVariable Long id, @RequestParam Role role) {
        return ApiResponse.success("Rôle modifié", userService.changeUserRole(id, role));
    }
}