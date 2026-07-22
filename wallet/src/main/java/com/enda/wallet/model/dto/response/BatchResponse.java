package com.enda.wallet.model.dto.response;

import com.enda.wallet.model.enums.BatchStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BatchResponse {
    private Long id;
    private String filename;
    private Integer totalLines;
    private Integer successfulLines;
    private Integer failedLines;
    private String status;
    private String initiatorUsername;
    private String validatorUsername;
    private LocalDateTime uploadedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime completedAt;
    private String reportPath;
    private List<TransactionResponse> transactions;
    private String validationRemark;

    // ✅ AJOUT : liste des erreurs détaillées
    private List<ErrorDetail> errors;

    // ✅ CLASSE INTERNE POUR LES DÉTAILS D'ERREUR
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ErrorDetail {
        private Integer line;
        private String field;
        private String message;
    }
}