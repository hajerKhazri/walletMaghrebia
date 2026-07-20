export interface VerifyWalletRequest {
  mobileNumber: string;
  userType: string;
}

export interface VerifyWalletResponse {
  exists: boolean;
  workspaceId: string;
  mobileNumber: string;
  userStatus: string;
  firstName: string;
  lastName: string;
  kycIdType: string;
  kycIdValue: string;
  traceId: string;
  errorCode: string;
  errorMessage: string;
}