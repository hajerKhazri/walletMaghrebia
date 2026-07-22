import { Transaction } from './transaction.model';
export enum BatchStatus {
  PENDING = 'PENDING',
  VALIDATED = 'VALIDATED',
  REJECTED = 'REJECTED',
  PROCESSING = 'PROCESSING',
  COMPLETED = 'COMPLETED'
}

export interface Batch {
  id: number;
  filename: string;
  totalLines: number;
  successfulLines: number;
  failedLines: number;
  status: BatchStatus;
  initiatorUsername: string;
  validatorUsername: string | null;
  uploadedAt: string;
  validatedAt: string | null;
  completedAt: string | null;
  validationRemark?: string;   // ✅ Ajouté
}