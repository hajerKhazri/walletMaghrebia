package com.enda.wallet.controller;

import com.enda.wallet.model.dto.request.LoginRequest;
import com.enda.wallet.model.dto.response.ApiResponse;
import com.enda.wallet.model.dto.response.LoginResponse;
import com.enda.wallet.model.dto.response.TransactionResponse;
import com.enda.wallet.service.AuthService;
import com.enda.wallet.service.BatchService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
@Slf4j
public class AuthController {

    private final AuthService authService;
    private final BatchService batchService;
    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        log.info("Tentative de connexion pour l'utilisateur: {}", request.getUsername());
        LoginResponse response = authService.login(request);
        return ApiResponse.success("Connexion réussie", response);
    }

    @PostMapping("/refresh")
    public ApiResponse<LoginResponse> refreshToken(@RequestBody String refreshToken) {
        log.info("Tentative de rafraîchissement du token");
        LoginResponse response = authService.refreshToken(refreshToken);
        return ApiResponse.success("Token rafraîchi avec succès", response);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(@RequestBody String token) {
        log.info("Déconnexion");
        authService.logout(token);
        return ApiResponse.success("Déconnexion réussie", null);
    }
    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'INITIATEUR')")
    public ApiResponse<List<TransactionResponse>> getBatchTransactions(@PathVariable Long id) {
        log.info("Consultation des transactions du batch {}", id);
        List<TransactionResponse> transactions = batchService.getBatchTransactions(id);
        return ApiResponse.success(transactions);
    }
}