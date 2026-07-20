import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { TransactionService } from '../../../../core/services/transaction.service';
import { WalletService } from '../../../../core/services/wallet.service';
import { ToastService } from '../../../../core/services/toast.service';
import { VerifyWalletResponse } from '../../../../core/models/wallet.model';

@Component({
  selector: 'app-transaction-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './transaction-create.component.html',
  styleUrls: ['./transaction-create.component.css']
})
export class TransactionCreateComponent {
  private fb = inject(FormBuilder);
  private transactionService = inject(TransactionService);
  private walletService = inject(WalletService);
  private toastService = inject(ToastService);
  public router = inject(Router);

  // ✅ Liste des motifs
  motifs: string[] = [
    'Frais de mission',
    'Frais de transport',
    'Dons et cadeaux',
    "Frais d'assurance",
    'Avances',
    'Prêts',
    'STC',
    'Salaires'
  ];

  transactionForm: FormGroup;
  loading = false;
  verifying = false;
  walletVerified = false;
  walletInfo: VerifyWalletResponse | null = null;

  constructor() {
    this.transactionForm = this.fb.group({
      mobileNumber: ['', [Validators.required, Validators.pattern('^[0-9]{8}$')]],
      userType: ['SUBSCRIBER', [Validators.required]],
      amount: ['', [Validators.required, Validators.min(0.01)]],
      motif: ['', [Validators.required]],
      autreMotif: ['']
    });
  }

  // ✅ AJOUTE CETTE MÉTHODE ICI
  getMotifLabel(): string {
    const motif = this.transactionForm.get('motif')?.value;
    if (motif === 'autre') {
      return this.transactionForm.get('autreMotif')?.value || 'Autre';
    }
    return motif || 'Non spécifié';
  }

  verifyWallet(): void {
    if (this.transactionForm.get('mobileNumber')?.invalid) {
      this.toastService.warning('Veuillez saisir un numéro de téléphone valide (8 chiffres)');
      return;
    }

    this.verifying = true;
    this.walletVerified = false;
    this.walletInfo = null;

    const request = {
      mobileNumber: this.transactionForm.get('mobileNumber')?.value,
      userType: this.transactionForm.get('userType')?.value
    };

    this.walletService.verifyWallet(request).subscribe({
      next: (response) => {
        if (response.data && response.data.exists) {
          this.walletVerified = true;
          this.walletInfo = response.data;
          this.toastService.success('Wallet vérifié avec succès');
        } else {
          this.toastService.error('Wallet non trouvé');
        }
        this.verifying = false;
      },
      error: () => {
        this.verifying = false;
        this.toastService.error('Erreur lors de la vérification');
      }
    });
  }

  onSubmit(): void {
    if (this.transactionForm.invalid) {
      this.toastService.warning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    if (!this.walletVerified) {
      this.toastService.warning('Veuillez d\'abord vérifier le wallet');
      return;
    }

    const formValue = this.transactionForm.value;
    let motifFinal = formValue.motif;
    if (motifFinal === 'autre') {
      motifFinal = formValue.autreMotif || 'Autre';
    }

    const payload = {
      mobileNumber: formValue.mobileNumber,
      userType: formValue.userType,
      amount: formValue.amount,
      remarks: motifFinal
    };

    this.loading = true;
    this.transactionService.createTransaction(payload).subscribe({
      next: (response) => {
        if (response.data) {
          this.toastService.success('Transaction créée avec succès');
          this.router.navigate(['/transactions', response.data.id]);
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Erreur lors de la création de la transaction');
      }
    });
  }
}