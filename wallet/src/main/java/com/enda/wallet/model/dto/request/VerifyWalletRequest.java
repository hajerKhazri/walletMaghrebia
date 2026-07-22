package com.enda.wallet.model.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyWalletRequest {

    @NotBlank(message = "Le numéro de téléphone est obligatoire")
    @Pattern(regexp = "^[0-9]{8}$", message = "Le numéro doit contenir 8 chiffres")
    private String mobileNumber;

    @NotBlank(message = "Le type d'utilisateur est obligatoire")
    private String userType;
}
