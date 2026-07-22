package com.enda.wallet.controller;

import com.enda.wallet.model.dto.request.VerifyWalletRequest;
import com.enda.wallet.model.dto.response.ApiResponse;
import com.enda.wallet.model.dto.response.VerifyWalletResponse;
import com.enda.wallet.service.BankToWalletClient;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/wallet")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Wallet", description = "Gestion des wallets")
@SecurityRequirement(name = "bearerAuth")
public class WalletController {

    private final BankToWalletClient bankToWalletClient;

    @PostMapping("/verify")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    @Operation(summary = "Vérifier l'existence d'un wallet")
    public ApiResponse<VerifyWalletResponse> verifyWallet(@Valid @RequestBody VerifyWalletRequest request) {
        log.info("Vérification du wallet pour le numéro: {}", request.getMobileNumber());
        VerifyWalletResponse response = bankToWalletClient.verifyUser(
                request.getMobileNumber(),
                request.getUserType()
        );
        return ApiResponse.success(response);
    }
}