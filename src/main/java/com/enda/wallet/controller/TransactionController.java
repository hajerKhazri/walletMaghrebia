package com.enda.wallet.controller;

import com.enda.wallet.model.dto.request.TransactionRequest;
import com.enda.wallet.model.dto.request.ValidateOtpRequest;
import com.enda.wallet.model.dto.response.ApiResponse;
import com.enda.wallet.model.dto.response.TransactionResponse;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.service.TransactionService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/transactions")
@RequiredArgsConstructor
@Slf4j
@Tag(name = "Transactions", description = "Gestion des transactions")
@SecurityRequirement(name = "bearerAuth")
public class TransactionController {

    private final TransactionService transactionService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    @Operation(summary = "Créer une transaction unitaire")
    public ApiResponse<TransactionResponse> createTransaction(
            @Valid @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Création d'une transaction par l'utilisateur: {}", currentUser.getUsername());
        TransactionResponse response = transactionService.createTransaction(request, currentUser);
        return ApiResponse.success("Transaction créée avec succès", response);
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter le détail d'une transaction")
    public ApiResponse<TransactionResponse> getTransaction(@PathVariable Long id) {
        log.info("Consultation de la transaction: {}", id);
        TransactionResponse response = transactionService.getTransaction(id);
        return ApiResponse.success(response);
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    @Operation(summary = "Consulter toutes les transactions (avec filtres)")
    public ApiResponse<Page<TransactionResponse>> getAllTransactions(
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String mobileNumber,
            @RequestParam(required = false) String fromDate,
            @RequestParam(required = false) String toDate,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TransactionResponse> transactions = transactionService.getAllTransactions(
                status, mobileNumber, fromDate, toDate, pageable
        );
        return ApiResponse.success(transactions);
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Consulter ses propres transactions")
    public ApiResponse<Page<TransactionResponse>> getMyTransactions(
            @AuthenticationPrincipal User currentUser,
            @PageableDefault(size = 20) Pageable pageable) {
        Page<TransactionResponse> transactions = transactionService.getMyTransactions(currentUser, pageable);
        return ApiResponse.success(transactions);
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN')")
    @Operation(summary = "Consulter les transactions en attente de validation")
    public ApiResponse<Page<TransactionResponse>> getPendingTransactions(
            @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(transactionService.getPendingTransactions(pageable));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN')")
    @Operation(summary = "Valider une transaction avec OTP")
    public ApiResponse<TransactionResponse> validateTransaction(
            @PathVariable Long id,
            @Valid @RequestBody ValidateOtpRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Validation de la transaction {} par l'utilisateur: {}", id, currentUser.getUsername());
        TransactionResponse response = transactionService.validateTransaction(id, request, currentUser);
        return ApiResponse.success("Transaction validée avec succès", response);
    }

    @PostMapping("/{id}/cancel")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    @Operation(summary = "Annuler une transaction")
    public ApiResponse<TransactionResponse> cancelTransaction(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        log.info("Annulation de la transaction {} par l'utilisateur: {}", id, currentUser.getUsername());
        TransactionResponse response = transactionService.cancelTransaction(id, currentUser);
        return ApiResponse.success("Transaction annulée avec succès", response);
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN')")
    @Operation(summary = "Rejeter une transaction")
    public ApiResponse<TransactionResponse> rejectTransaction(
            @PathVariable Long id,
            @RequestParam String reason,
            @AuthenticationPrincipal User currentUser) {
        log.info("Rejet de la transaction {} par l'utilisateur: {}", id, currentUser.getUsername());
        TransactionResponse response = transactionService.rejectTransaction(id, reason, currentUser);
        return ApiResponse.success("Transaction rejetée", response);
    }

    // ✅ CORRIGÉ : plus de paramètre currentUser
    @PostMapping("/{id}/initiate-validation")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    @Operation(summary = "Initier la validation d'une transaction (envoi OTP)")
    public ApiResponse<Void> initiateValidation(
            @PathVariable Long id) {
        log.info("Initiation de la validation pour la transaction: {}", id);
        transactionService.initiateValidation(id);   // ✅ appel corrigé
        return ApiResponse.success("OTP envoyé avec succès au validateur", null);
    }

    @PostMapping("/{id}/check-status")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    @Operation(summary = "Vérifier le statut d'une transaction auprès de l'API externe")
    public ApiResponse<TransactionResponse> checkExternalStatus(@PathVariable Long id) {
        log.info("Vérification du statut externe pour la transaction: {}", id);
        TransactionResponse response = transactionService.checkExternalStatus(id);
        return ApiResponse.success(response);
    }






    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    public ApiResponse<TransactionResponse> updateTransaction(
            @PathVariable Long id,
            @RequestBody TransactionRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Modification de la transaction {} par {}", id, currentUser.getUsername());
        TransactionResponse response = transactionService.updateTransaction(id, request, currentUser);
        return ApiResponse.success("Transaction mise à jour", response);
    }


}