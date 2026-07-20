import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ActivatedRoute, Router, RouterModule } from '@angular/router';
import { TransactionService } from '../../../../core/services/transaction.service';
import { AuthService } from '../../../../core/services/auth.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Transaction, TransactionStatus } from '../../../../core/models/transaction.model';

@Component({
  selector: 'app-transaction-detail',
  standalone: true,
  imports: [CommonModule, FormsModule, RouterModule],
  templateUrl: './transaction-detail.component.html',
  styleUrls: ['./transaction-detail.component.css']
})
export class TransactionDetailComponent implements OnInit {
  private route = inject(ActivatedRoute);
  private router = inject(Router);
  private transactionService = inject(TransactionService);
  private authService = inject(AuthService);
  private toastService = inject(ToastService);

  transaction: Transaction | null = null;
  loading = true;
  validating = false;
  otpCode: string = '';
  validationRemark: string = '';

  // ✅ Propriétés pour le modal de rejet
  showRejectModal = false;
  rejectReason: string = '';

  ngOnInit(): void {
    const id = this.route.snapshot.params['id'];
    if (id) {
      this.loadTransaction(id);
    }
  }

  loadTransaction(id: number): void {
    this.loading = true;
    this.transactionService.getTransaction(id).subscribe({
      next: (response) => {
        if (response.data) {
          this.transaction = response.data;
        } else {
          this.toastService.error('Transaction non trouvée');
          this.router.navigate(['/transactions']);
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Erreur lors du chargement de la transaction');
        this.router.navigate(['/transactions']);
      }
    });
  }

  // ============================================
  // MÉTHODES DE VALIDATION
  // ============================================

  canValidate(): boolean {
    return this.authService.hasRole('VALIDATEUR') || this.authService.hasRole('ADMIN');
  }

  canCancel(): boolean {
    if (!this.transaction) return false;
    const status = this.transaction.status;
    return (status === 'CREATED' || status === 'AWAITING_VALIDATION') &&
           (this.authService.hasRole('INITIATEUR') || this.authService.hasRole('ADMIN'));
  }

  canReject(): boolean {
    if (!this.transaction) return false;
    return this.transaction.status === 'AWAITING_VALIDATION' &&
           (this.authService.hasRole('VALIDATEUR') || this.authService.hasRole('ADMIN'));
  }

  canInitiateValidation(): boolean {
    if (!this.transaction) return false;
    return this.transaction.status === 'CREATED' &&
           (this.authService.hasRole('INITIATEUR') || this.authService.hasRole('ADMIN'));
  }

  // ✅ Validation avec vérification de nullabilité
  validateTransaction(): void {
    // Vérifier que la transaction existe
    if (!this.transaction) {
      this.toastService.error('Transaction introuvable');
      return;
    }

    // Vérifier l'OTP
    if (!this.otpCode || this.otpCode.length !== 6) {
      this.toastService.warning('Veuillez saisir un code OTP à 6 chiffres');
      return;
    }

    this.validating = true;

    const payload = {
      otp: this.otpCode.trim(),
      remark: this.validationRemark?.trim() || '',
      transactionId: this.transaction.id  // ✅ Maintenant `this.transaction` est non null
    };

    console.log('📤 Payload envoyé :', payload);

    // Stocker l'ID localement pour l'utiliser après
    const transactionId = this.transaction.id;

    this.transactionService.validateTransaction(transactionId, payload).subscribe({
      next: (response) => {
        if (response.data) {
          this.transaction = response.data;
          this.toastService.success('✅ Transaction validée avec succès');
          this.otpCode = '';
          this.validationRemark = '';
          this.validating = false;
          // Recharger la transaction pour avoir les dernières données
          this.loadTransaction(transactionId);
        } else {
          this.validating = false;
          this.toastService.error('Erreur lors de la validation');
        }
      },
      error: (err) => {
        this.validating = false;
        const msg = err.error?.errorMessage || err.message || 'Erreur lors de la validation';
        this.toastService.error(msg);
        console.error('❌ Erreur validation :', err);
      }
    });
  }

  // ============================================
  // MÉTHODES DE REJET
  // ============================================

  openRejectModal(): void {
    this.showRejectModal = true;
    this.rejectReason = '';
  }

  closeRejectModal(): void {
    this.showRejectModal = false;
    this.rejectReason = '';
  }

  confirmReject(): void {
    if (!this.rejectReason || this.rejectReason.trim() === '') {
      this.toastService.warning('Veuillez saisir un motif de rejet');
      return;
    }
    this.rejectTransaction(this.rejectReason);
    this.closeRejectModal();
  }

  rejectTransaction(reason?: string): void {
    if (!this.transaction) {
      this.toastService.error('Transaction introuvable');
      return;
    }
    const motif = reason || this.rejectReason || 'Rejeté sans motif';
    const transactionId = this.transaction.id;

    this.transactionService.rejectTransaction(transactionId, motif).subscribe({
      next: (response) => {
        if (response.data) {
          this.transaction = response.data;
          this.toastService.success('Transaction rejetée');
          this.loadTransaction(transactionId);
        }
      },
      error: (err) => {
        const msg = err.error?.errorMessage || err.message || 'Erreur lors du rejet';
        this.toastService.error(msg);
      }
    });
  }

  // ============================================
  // MÉTHODES D'ANNULATION
  // ============================================

  cancelTransaction(): void {
    if (!this.transaction) {
      this.toastService.error('Transaction introuvable');
      return;
    }
    if (!confirm('Voulez-vous vraiment annuler cette transaction ?')) return;

    const transactionId = this.transaction.id;
    this.transactionService.cancelTransaction(transactionId).subscribe({
      next: (response) => {
        if (response.data) {
          this.transaction = response.data;
          this.toastService.success('Transaction annulée');
          this.loadTransaction(transactionId);
        }
      },
      error: (err) => {
        const msg = err.error?.errorMessage || err.message || 'Erreur lors de l\'annulation';
        this.toastService.error(msg);
      }
    });
  }

  // ============================================
  // MÉTHODES D'ENVOI OTP
  // ============================================

  initiateValidation(): void {
    if (!this.transaction) {
      this.toastService.error('Transaction introuvable');
      return;
    }

    this.loading = true;
    const transactionId = this.transaction.id;
    this.transactionService.initiateValidation(transactionId).subscribe({
      next: () => {
        this.toastService.success('OTP envoyé avec succès à votre email');
        this.loadTransaction(transactionId);
        this.loading = false;
      },
      error: (err) => {
        this.loading = false;
        const msg = err.error?.errorMessage || err.message || 'Erreur lors de l\'envoi de l\'OTP';
        this.toastService.error(msg);
      }
    });
  }

  // ============================================
  // HELPERS
  // ============================================

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