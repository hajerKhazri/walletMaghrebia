export enum TransactionStatus {
  CREATED = 'CREATED',
  AWAITING_VALIDATION = 'AWAITING_VALIDATION',
  VALIDATED = 'VALIDATED',
  PROCESSING = 'PROCESSING',
  SUCCEEDED = 'SUCCEEDED',
  FAILED = 'FAILED',
  REJECTED = 'REJECTED',
  CANCELLED = 'CANCELLED',
  TIMEOUT = 'TIMEOUT'
}

export interface Transaction {
  id: number;
  refId: string;
  txnId: string | null;
  mobileNumber: string;
  amount: number;
  remarks: string | null;
  status: TransactionStatus;
  initiatorUsername: string;
  validatorUsername: string | null;
  initiatedAt: string;
  validatedAt: string | null;
  completedAt: string | null;
  errorCode: string | null;
  errorMessage: string | null;
  batchId: number | null;
}

export interface TransactionCreateRequest {
  mobileNumber: string;
  amount: number;
  remarks: string;
  userType: string;
}

// ✅ Correction : le backend attend "otp" (pas "otpCode")
export interface ValidateOtpRequest {
  otp: string;   // ← changé
}

export interface TransactionFilter {
  status?: TransactionStatus;
  mobileNumber?: string;
  fromDate?: string;
  toDate?: string;
  page?: number;
  size?: number;
}

export interface Page<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
  first: boolean;
  last: boolean;
  empty: boolean;
}