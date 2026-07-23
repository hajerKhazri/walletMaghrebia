package com.enda.wallet.model.dto.request;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class TransactionRequest {

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[0-9]{8}$", message = "Le numéro doit contenir 8 chiffres")
    private String mobileNumber;
    private String otp;
    private String code;
    @NotNull(message = "Le montant est obligatoire")
    @DecimalMin(value = "0.01", message = "Le montant doit être supérieur à 0")
    @DecimalMax(value = "1000000", message = "Le montant ne peut pas dépasser 1 000 000")
    private BigDecimal amount;

    @Size(max = 500, message = "Le motif ne peut pas dépasser 500 caractères")
    private String remarks;

    @NotBlank(message = "Le type d'utilisateur est obligatoire")
    private String userType;
}
