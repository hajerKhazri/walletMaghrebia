package com.enda.wallet.model.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class VerifyWalletResponse {
    private boolean exists;
    private String workspaceId;
    private String mobileNumber;
    private String userStatus;
    private String firstName;
    private String lastName;
    private String kycIdType;
    private String kycIdValue;
    private String traceId;
    private String errorCode;
    private String errorMessage;
}