export interface ApiResponse<T> {
  status: number;
  message: string;
  data: T;
  errorCode: string | null;
  errorMessage: string | null;
  timestamp: string;
}