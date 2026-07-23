package com.enda.wallet.model.dto.response;

import com.enda.wallet.model.enums.TransactionStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TransactionResponse {
    private Long id;
    private String refId;
    private String txnId;
    private String mobileNumber;
    private BigDecimal amount;
    private String remarks;
    private TransactionStatus status;
    private String initiatorUsername;
    private String validatorUsername;
    private LocalDateTime initiatedAt;
    private LocalDateTime validatedAt;
    private LocalDateTime completedAt;
    private String externalTraceId;
    private String errorCode;
    private String errorMessage;
    private Long batchId;
    private String validationRemark;
}