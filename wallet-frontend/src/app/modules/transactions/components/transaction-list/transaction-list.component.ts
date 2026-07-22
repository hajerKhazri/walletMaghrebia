import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { TransactionService } from '../../../../core/services/transaction.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Transaction, TransactionStatus, TransactionFilter } from '../../../../core/models/transaction.model';

@Component({
  selector: 'app-transaction-list',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './transaction-list.component.html',
  styleUrls: ['./transaction-list.component.css']
})
export class TransactionListComponent implements OnInit {
  Math = Math;

  private transactionService = inject(TransactionService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  transactions: Transaction[] = [];
  loading = false;
  filters: TransactionFilter = {
    status: undefined,
    mobileNumber: '',
    fromDate: '',
    toDate: ''
  };
  pageInfo: any;
  pageNumbers: number[] = [];
  statusOptions: string[] = Object.values(TransactionStatus);
  isAdmin = false;
  isValidator = false;
  isInitiateur = false;
  currentPage = 0;
  pageSize = 20;

  // Modal de confirmation
  showConfirmModal = false;
  confirmMessage = '';
  confirmHint = '';
  confirmButtonText = 'Confirmer';
  confirmCallback: (() => void) | null = null;

  ngOnInit(): void {
    const user = this.authService.getCurrentUser();
    this.isAdmin = user?.role === 'ADMIN';
    this.isValidator = user?.role === 'VALIDATEUR';
    this.isInitiateur = user?.role === 'INITIATEUR';
    this.loadTransactions();
  }

  loadTransactions(): void {
    this.loading = true;
    let obs;

    if (this.isValidator) {
      // Validateur → transactions en attente
      obs = this.transactionService.getPendingTransactions(this.currentPage, this.pageSize);
    } else if (this.isInitiateur) {
      // Initiateur → ses propres transactions
      obs = this.transactionService.getMyTransactions(this.currentPage, this.pageSize);
    } else {
      // ADMIN / FINANCE → toutes avec filtres
      obs = this.transactionService.getAllTransactions(this.filters, this.currentPage, this.pageSize);
    }

    obs.subscribe({
      next: (response) => {
        if (response.data) {
          this.transactions = response.data.content;
          this.pageInfo = response.data;
          this.pageNumbers = Array.from({ length: this.pageInfo.totalPages }, (_, i) => i);
        } else {
          this.transactions = [];
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Erreur lors du chargement des transactions');
      }
    });
  }

  // Filtres
  applyFilters(): void {
    this.currentPage = 0; // Revenir à la première page
    this.loadTransactions();
  }

  resetFilters(): void {
    this.filters = { status: undefined, mobileNumber: '', fromDate: '', toDate: '' };
    this.currentPage = 0;
    this.loadTransactions();
  }

  changePage(page: number): void {
    if (page < 0 || page >= this.pageInfo.totalPages) return;
    this.currentPage = page;
    this.loadTransactions();
  }

  // Actions
  canEdit(transaction: Transaction): boolean {
    const user = this.authService.getCurrentUser();
    if (!user) return false;
    return user.username === transaction.initiatorUsername || user.role === 'ADMIN';
  }

  editRemarks(transaction: Transaction): void {
    const currentRemarks = transaction.remarks || '';
    const newRemarks = prompt('Nouveau motif :', currentRemarks);
    if (newRemarks !== null && newRemarks !== currentRemarks) {
      this.transactionService.updateTransaction(transaction.id, { remarks: newRemarks })
        .subscribe({
          next: () => {
            this.toastService.success('Motif mis à jour');
            this.loadTransactions();
          },
          error: () => this.toastService.error('Erreur lors de la mise à jour')
        });
    }
  }

  openCancelConfirm(transaction: Transaction): void {
    this.openConfirmModal(
      `Annuler la transaction #${transaction.id} ?`,
      'Cette action est irréversible.',
      'Annuler',
      () => {
        this.transactionService.cancelTransaction(transaction.id).subscribe({
          next: () => {
            this.toastService.success('Transaction annulée');
            this.loadTransactions();
          },
          error: () => this.toastService.error('Erreur lors de l\'annulation')
        });
      }
    );
  }

  // Modal
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

  // Helpers
  getStatusClass(status: string): string {
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

  getStatusLabel(status: string): string {
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
    return date ? new Date(date).toLocaleString('fr-FR') : '-';
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'TND' }).format(amount);
  }
}