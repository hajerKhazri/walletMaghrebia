import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BatchService } from '../../../../core/services/batch.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Batch, BatchStatus } from '../../../../core/models/batch.model';
import { Transaction, TransactionStatus } from '../../../../core/models/transaction.model';

@Component({
  selector: 'app-batch-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './batch-list.component.html',
  styleUrls: ['./batch-list.component.css']
})
export class BatchListComponent implements OnInit {
  private batchService = inject(BatchService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  batches: Batch[] = [];
  loading = true;
  isAdmin = false;
  canExport = false;
  currentUsername = '';

  validating = false;
  showOtpModal = false;
  currentBatchId: number | null = null;
  otpInput: string = '';
  otpError: string = '';
  batchValidationRemark: string = '';

  showTransactionsModal = false;
  selectedBatch: Batch | null = null;
  batchTransactions: Transaction[] = [];
  transactionsLoading = false;

  // Propriétés pour le modal de confirmation
  showConfirmModal = false;
  confirmMessage = '';
  confirmHint = '';
  confirmButtonText = 'Confirmer';
  confirmCallback: (() => void) | null = null;

  ngOnInit(): void {
    this.isAdmin = this.authService.hasRole('ADMIN');
    this.canExport = this.authService.hasRole('FINANCE') || this.authService.hasRole('ADMIN');
    this.currentUsername = this.authService.getCurrentUser()?.username || '';
    this.loadBatches();
  }

  loadBatches(): void {
    this.loading = true;
    const observer = {
      next: (response: any) => {
        if (response.data && response.data.content) {
          this.batches = response.data.content;
        } else if (Array.isArray(response.data)) {
          this.batches = response.data;
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Erreur lors du chargement des batches');
      }
    };

    if (this.authService.hasRole('ADMIN') || this.authService.hasRole('VALIDATEUR') || this.authService.hasRole('FINANCE')) {
      this.batchService.getAllBatches(0, 100).subscribe(observer);
    } else {
      this.batchService.getMyBatches(0, 100).subscribe(observer);
    }
  }

  // ============================================
  // NORMALISATION DU STATUT
  // ============================================
  private normalizeStatus(status: any): string {
    if (typeof status === 'string') {
      return status.toUpperCase();
    }
    if (typeof status === 'boolean') {
      return status ? 'VALIDATED' : 'PENDING';
    }
    if (typeof status === 'number') {
      const map: Record<number, string> = {
        0: 'PENDING',
        1: 'VALIDATED',
        2: 'REJECTED',
        3: 'COMPLETED'
      };
      return map[status] || 'PENDING';
    }
    return 'PENDING';
  }

  isStatus(batch: Batch, statusKey: string): boolean {
    return this.normalizeStatus(batch.status) === statusKey;
  }

  getStatusLabel(status: any): string {
    const key = this.normalizeStatus(status);
    const map: Record<string, string> = {
      'PENDING': 'En attente',
      'VALIDATED': 'Validé',
      'REJECTED': 'Rejeté',
      'PROCESSING': 'En cours',
      'COMPLETED': 'Terminé'
    };
    return map[key] || key;
  }

  getStatusClass(status: any): string {
    const key = this.normalizeStatus(status);
    const map: Record<string, string> = {
      'PENDING': 'badge-warning',
      'VALIDATED': 'badge-primary',
      'REJECTED': 'badge-danger',
      'PROCESSING': 'badge-info',
      'COMPLETED': 'badge-success'
    };
    return map[key] || 'badge-secondary';
  }

  // ============================================
  // VALIDATION OTP
  // ============================================
  validateBatch(batch: Batch): void {
    console.log('🟢 validateBatch appelé pour le batch ID :', batch.id);
    this.batchService.initiateValidation(batch.id).subscribe({
      next: () => {
        this.currentBatchId = batch.id;
        this.otpInput = '';
        this.otpError = '';
        this.batchValidationRemark = '';
        this.showOtpModal = true;
      },
      error: (err) => {
        console.error('❌ Erreur initiateValidation :', err);
        const errorMsg = err.error?.errorMessage || err.message || 'Erreur lors de la validation';
        this.toastService.error('❌ ' + errorMsg);
      }
    });
  }

  confirmOtp(): void {
    if (!this.otpInput || this.otpInput.length !== 6) {
      this.otpError = 'Veuillez saisir un code OTP à 6 chiffres.';
      return;
    }
    this.otpError = '';

    if (this.currentBatchId === null) {
      this.toastService.error('Batch invalide');
      return;
    }

    this.validating = true;
    const payload = { otp: this.otpInput, remark: this.batchValidationRemark };

    this.batchService.validateBatch(this.currentBatchId, payload).subscribe({
      next: (response) => {
        if (response.data) {
          const index = this.batches.findIndex(b => b.id === this.currentBatchId);
          if (index !== -1) this.batches[index] = response.data;
          this.toastService.success('✅ Batch validé avec succès');
          this.closeOtpModal();
          this.loadBatches();
        }
        this.validating = false;
      },
      error: (err) => {
        this.validating = false;
        const errorMsg = err.error?.errorMessage || err.message || 'Erreur lors de la validation';
        this.otpError = errorMsg;
        this.toastService.error(errorMsg);
        console.error('❌ Erreur validation batch :', err);
      }
    });
  }

  closeOtpModal(): void {
    this.showOtpModal = false;
    this.currentBatchId = null;
    this.otpInput = '';
    this.otpError = '';
    this.batchValidationRemark = '';
    this.validating = false;
  }

  // ============================================
  // MODAL DE CONFIRMATION
  // ============================================
  openConfirmModal(message: string, hint: string = '', buttonText: string = 'Confirmer', callback: () => void): void {
    this.confirmMessage = message;
    this.confirmHint = hint;
    this.confirmButtonText = buttonText;
    this.confirmCallback = callback;
    this.showConfirmModal = true;
  }

  closeConfirmModal(): void {
    this.showConfirmModal = false;
    this.confirmCallback = null;
  }

  confirmAction(): void {
    if (this.confirmCallback) {
      this.confirmCallback();
    }
    this.closeConfirmModal();
  }

  // ============================================
  // ACTIONS (DELETE, REJECT, EXPORT, ETC.)
  // ============================================
  deleteBatch(batch: Batch): void {
    this.openConfirmModal(
      `Supprimer le batch #${batch.id} ?`,
      'Cette action est irréversible.',
      'Supprimer',
      () => {
        this.batchService.deleteBatch(batch.id).subscribe({
          next: () => {
            this.toastService.success('Batch supprimé');
            this.loadBatches();
          },
          error: () => this.toastService.error('Erreur lors de la suppression')
        });
      }
    );
  }

  rejectBatch(batch: Batch): void {
    const reason = prompt('Motif du rejet :');
    if (reason === null) return;
    this.batchService.rejectBatch(batch.id, reason || 'Non spécifié').subscribe({
      next: (response) => {
        if (response.data) {
          const index = this.batches.findIndex(b => b.id === batch.id);
          if (index !== -1) this.batches[index] = response.data;
          this.toastService.success('Batch rejeté');
        }
      },
      error: () => this.toastService.error('Erreur lors du rejet')
    });
  }

  exportBatches(): void {
    if (!this.canExport) {
      this.toastService.warning('Droits insuffisants');
      return;
    }
    this.batchService.exportBatches().subscribe({
      next: (blob) => {
        const link = document.createElement('a');
        link.href = URL.createObjectURL(blob);
        link.download = 'batches_export.csv';
        link.click();
        this.toastService.success('Export téléchargé');
      },
      error: () => this.toastService.error('Erreur export')
    });
  }

  // ============================================
  // TRANSACTIONS MODAL
  // ============================================
  viewBatchTransactions(batch: Batch): void {
    this.selectedBatch = batch;
    this.transactionsLoading = true;
    this.batchService.getBatchTransactions(batch.id).subscribe({
      next: (response) => {
        this.batchTransactions = response.data || [];
        this.showTransactionsModal = true;
        this.transactionsLoading = false;
      },
      error: () => {
        this.toastService.error('Erreur chargement des transactions');
        this.transactionsLoading = false;
      }
    });
  }

  closeTransactionsModal(): void {
    this.showTransactionsModal = false;
    this.selectedBatch = null;
    this.batchTransactions = [];
  }

  // ============================================
  // PERMISSIONS
  // ============================================
  canValidate(batch: Batch): boolean {
    return this.isStatus(batch, 'PENDING') &&
           (this.authService.hasRole('VALIDATEUR') || this.authService.hasRole('ADMIN'));
  }

  canReject(batch: Batch): boolean {
    return this.isStatus(batch, 'PENDING') &&
           (this.authService.hasRole('VALIDATEUR') || this.authService.hasRole('ADMIN'));
  }

  // ============================================
  // HELPERS
  // ============================================
  getTransactionStatusClass(status: string): string {
    const map: Record<string, string> = {
      'CREATED': 'badge-warning',
      'AWAITING_VALIDATION': 'badge-info',
      'VALIDATED': 'badge-primary',
      'PROCESSING': 'badge-info',
      'SUCCEEDED': 'badge-success',
      'FAILED': 'badge-danger',
      'REJECTED': 'badge-danger',
      'CANCELLED': 'badge-secondary',
      'TIMEOUT': 'badge-warning'
    };
    return map[status] || 'badge-secondary';
  }

  getTransactionStatusLabel(status: string): string {
    const map: Record<string, string> = {
      'CREATED': 'Créée',
      'AWAITING_VALIDATION': 'En attente de validation',
      'VALIDATED': 'Validée',
      'PROCESSING': 'En cours',
      'SUCCEEDED': 'Succès',
      'FAILED': 'Échec',
      'REJECTED': 'Rejetée',
      'CANCELLED': 'Annulée',
      'TIMEOUT': 'Expirée'
    };
    return map[status] || status;
  }

  formatDate(date: string): string {
    if (!date) return '-';
    return new Date(date).toLocaleString('fr-FR');
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', {
      style: 'currency',
      currency: 'TND'
    }).format(amount);
  }
}