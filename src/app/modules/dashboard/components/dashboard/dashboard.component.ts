import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule } from '@angular/router';
import { AuthService } from '../../../../core/services/auth.service';
import { TransactionService } from '../../../../core/services/transaction.service';
import { UserService } from '../../../../core/services/user.service';
import { Transaction, TransactionStatus } from '../../../../core/models/transaction.model';
import { FormsModule } from '@angular/forms';
@Component({
  selector: 'app-dashboard',
  standalone: true,
   imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './dashboard.component.html',   // ✅ corrigé
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  public authService = inject(AuthService);
  private transactionService = inject(TransactionService);
  private userService = inject(UserService);

  user: any;
  loading = true;
  balance = 0;
  showBalance = false;

  stats = {
    totalTransactions: 0,
    pendingTransactions: 0,
    successfulTransactions: 0,
    failedTransactions: 0,
    totalAmount: 0
  };
  recentTransactions: Transaction[] = [];

  ngOnInit(): void {
    this.user = this.authService.getCurrentUser();
    this.showBalance = this.user?.role === 'INITIATEUR' || this.user?.role === 'ADMIN';
    if (this.showBalance) {
      this.loadBalance();
    }
    this.loadDashboardData();
  }

  loadBalance(): void {
    this.userService.getBalance().subscribe({
      next: (data) => this.balance = data,
      error: () => this.balance = 0
    });
  }

  loadDashboardData(): void {
    this.loading = true;

    if (this.user?.role === 'VALIDATEUR') {
      this.transactionService.getPendingTransactions(0, 10).subscribe({
        next: (response) => {
          if (response.data) {
            this.recentTransactions = response.data.content;
            this.calculateStats(response.data.content);
          }
          this.loading = false;
        },
        error: () => this.loading = false
      });
    } else {
      this.transactionService.getMyTransactions(0, 10).subscribe({
        next: (response) => {
          if (response.data) {
            this.recentTransactions = response.data.content;
            this.calculateStats(response.data.content);
          }
          this.loading = false;
        },
        error: () => this.loading = false
      });
    }
  }

  calculateStats(transactions: Transaction[]): void {
    this.stats.totalTransactions = transactions.length;
    this.stats.pendingTransactions = transactions.filter(
      t => t.status === TransactionStatus.CREATED || t.status === TransactionStatus.AWAITING_VALIDATION
    ).length;
    this.stats.successfulTransactions = transactions.filter(
      t => t.status === TransactionStatus.SUCCEEDED
    ).length;
    this.stats.failedTransactions = transactions.filter(
      t => t.status === TransactionStatus.FAILED || t.status === TransactionStatus.REJECTED
    ).length;
    this.stats.totalAmount = transactions.reduce((sum, t) => sum + t.amount, 0);
  }

  getStatusClass(status: string): string {
    const map: Record<string,string> = {
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
    const map: Record<string,string> = {
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

  getRoleLabel(role: string): string {
    const map: Record<string,string> = {
      'ADMIN': 'Administrateur',
      'INITIATEUR': 'Initiateur',
      'VALIDATEUR': 'Validateur',
      'FINANCE': 'Finance'
    };
    return map[role] || role;
  }

  formatDate(date: string): string {
    return date ? new Date(date).toLocaleString('fr-FR') : '-';
  }

  formatAmount(amount: number): string {
    return new Intl.NumberFormat('fr-FR', { style: 'currency', currency: 'TND' }).format(amount);
  }
}