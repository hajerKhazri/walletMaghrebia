package com.enda.wallet.controller;

import com.enda.wallet.model.dto.request.BatchUpdateRequest;
import com.enda.wallet.model.dto.request.ValidateOtpRequest;
import com.enda.wallet.model.dto.response.ApiResponse;
import com.enda.wallet.model.dto.response.BatchResponse;
import com.enda.wallet.model.dto.response.TransactionResponse;
import com.enda.wallet.model.entity.Batch;
import com.enda.wallet.model.entity.User;
import com.enda.wallet.service.BatchService;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpHeaders;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;

@RestController
@RequestMapping("/batches")
@RequiredArgsConstructor
@Slf4j
public class BatchController {

    private final BatchService batchService;

    @PostMapping
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    public ApiResponse<BatchResponse> uploadBatch(@RequestParam("file") MultipartFile file,
                                                  @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Batch uploadé", batchService.uploadBatch(file, currentUser));
    }

    @GetMapping("/{id}")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<BatchResponse> getBatch(@PathVariable Long id) {
        return ApiResponse.success(batchService.getBatch(id));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN', 'VALIDATEUR')")
    public ApiResponse<Page<BatchResponse>> getAllBatches(Pageable pageable) {
        return ApiResponse.success(batchService.getAllBatches(pageable));
    }

    @GetMapping("/my")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<Page<BatchResponse>> getMyBatches(@AuthenticationPrincipal User currentUser,
                                                         @PageableDefault(size = 20) Pageable pageable) {
        return ApiResponse.success(batchService.getMyBatches(currentUser, pageable));
    }

    @PostMapping("/{id}/validate")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN')")
    public ApiResponse<BatchResponse> validateBatch(@PathVariable Long id,
                                                    @RequestBody ValidateOtpRequest request,
                                                    @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Batch validé", batchService.validateBatch(id, request, currentUser));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN')")
    public ApiResponse<BatchResponse> rejectBatch(@PathVariable Long id,
                                                  @RequestParam String reason,
                                                  @AuthenticationPrincipal User currentUser) {
        return ApiResponse.success("Batch rejeté", batchService.rejectBatch(id, reason, currentUser));
    }

    @GetMapping("/{id}/report")
    @PreAuthorize("isAuthenticated()")
    public ApiResponse<String> downloadReport(@PathVariable Long id) {
        return ApiResponse.success("Rapport généré", batchService.generateReport(id));
    }

    @GetMapping("/export")
    @PreAuthorize("hasAnyRole('FINANCE', 'ADMIN')")
    public void exportBatches(HttpServletResponse response) throws IOException {
        String csv = batchService.exportCsv();   // ✅ Appel unique
        response.setContentType("text/csv");
        response.setHeader(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=batches_export.csv");
        PrintWriter writer = response.getWriter();
        writer.write(csv);
        writer.flush();
    }



    // ─── MODIFIER un batch (nom du fichier, si statut PENDING) ───
    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    public ApiResponse<BatchResponse> updateBatch(
            @PathVariable Long id,
            @RequestBody BatchUpdateRequest request,
            @AuthenticationPrincipal User currentUser) {
        log.info("Modification du batch {} par {}", id, currentUser.getUsername());
        BatchResponse response = batchService.updateBatch(id, request, currentUser);
        return ApiResponse.success("Batch mis à jour", response);
    }

    // ─── SUPPRIMER un batch (uniquement si statut PENDING) ───
    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('INITIATEUR', 'ADMIN')")
    public ApiResponse<Void> deleteBatch(
            @PathVariable Long id,
            @AuthenticationPrincipal User currentUser) {
        log.info("Suppression du batch {} par {}", id, currentUser.getUsername());
        batchService.deleteBatch(id, currentUser);
        return ApiResponse.success("Batch supprimé", null);
    }

    @PostMapping("/{id}/initiate-validation")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN')")
    public ApiResponse<Void> initiateValidation(@PathVariable Long id,
                                                @AuthenticationPrincipal User currentUser) {
        log.info("Initiation de la validation pour le batch {} par {}", id, currentUser.getUsername());
        batchService.initiateValidation(id, currentUser);
        return ApiResponse.success("OTP envoyé au validateur", null);
    }
    @GetMapping("/{id}/transactions")
    @PreAuthorize("hasAnyRole('VALIDATEUR', 'ADMIN', 'INITIATEUR')")
    public ApiResponse<List<TransactionResponse>> getBatchTransactions(@PathVariable Long id) {
        log.info("Consultation des transactions du batch {}", id);
        List<TransactionResponse> transactions = batchService.getBatchTransactions(id);
        return ApiResponse.success(transactions);
    }
}