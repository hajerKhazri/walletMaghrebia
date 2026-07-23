package com.enda.wallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ValidateOtpRequest {

    @NotNull(message = "L'ID de la transaction est obligatoire")
    private Long transactionId;

    @NotBlank(message = "Le code OTP est obligatoire")
    @Size(min = 6, max = 6, message = "Le code OTP doit contenir exactement 6 chiffres")
    @Pattern(regexp = "^[0-9]{6}$", message = "Le code OTP doit contenir uniquement des chiffres")
    private String otp;
    private String remark;
}