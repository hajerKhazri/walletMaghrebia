package com.enda.wallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class BatchExportDto {
    private Long id;
    private LocalDateTime createdAt;
    private String status;
    private int totalLines;
    private BigDecimal totalAmount;
    private String initiatorUsername;
    private String validatorUsername;
    private String rejectionReason;
}