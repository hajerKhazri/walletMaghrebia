import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { FormsModule } from '@angular/forms';
import { BatchService } from '../../../../core/services/batch.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Batch } from '../../../../core/models/batch.model';

interface ErrorDetail {
  line: number;
  field: string;
  message: string;
}

@Component({
  selector: 'app-batch-upload',
  standalone: true,
  imports: [CommonModule, RouterModule, FormsModule],
  templateUrl: './batch-upload.component.html',
  styleUrls: ['./batch-upload.component.css']
})
export class BatchUploadComponent {
  private batchService = inject(BatchService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  selectedFile: File | null = null;
  uploading = false;
  dragOver = false;

  // Propriétés pour les erreurs
  hasErrors = false;
  errorList: ErrorDetail[] = [];
  errorSummary = '';
  totalLines = 0;
  showErrorModal = false;

  onFileSelected(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (input.files && input.files.length > 0) {
      const file = input.files[0];
      if (file.name.endsWith('.csv')) {
        this.selectedFile = file;
        this.toastService.info(`✅ Fichier sélectionné: ${file.name}`);
        this.clearErrors();
      } else {
        this.toastService.error('❌ Veuillez sélectionner un fichier CSV');
        this.selectedFile = null;
      }
    }
  }

  onDragOver(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = true;
  }

  onDragLeave(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
  }

  onDrop(event: DragEvent): void {
    event.preventDefault();
    this.dragOver = false;
    const files = event.dataTransfer?.files;
    if (files && files.length > 0) {
      const file = files[0];
      if (file.name.endsWith('.csv')) {
        this.selectedFile = file;
        this.toastService.info(`✅ Fichier sélectionné: ${file.name}`);
        this.clearErrors();
      } else {
        this.toastService.error('❌ Veuillez sélectionner un fichier CSV');
      }
    }
  }

  removeFile(): void {
    this.selectedFile = null;
    this.clearErrors();
  }

  clearErrors(): void {
    this.hasErrors = false;
    this.errorList = [];
    this.errorSummary = '';
    this.totalLines = 0;
    this.showErrorModal = false;
  }

  downloadTemplate(): void {
    const headers = 'msisdn,matricule,montant,prenom,nom,remark\n';
    const example = '89089000,P0153,850.000,Khaoula,ABDAOUI,scolarité\n';
    const content = headers + example;
    const blob = new Blob([content], { type: 'text/csv;charset=utf-8;' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = 'template_batch.csv';
    link.click();
    URL.revokeObjectURL(link.href);
    this.toastService.success('📄 Template téléchargé');
  }

  uploadBatch(): void {
    if (!this.selectedFile) {
      this.toastService.warning('⚠️ Veuillez sélectionner un fichier');
      return;
    }

    this.uploading = true;
    this.clearErrors();

    this.batchService.uploadBatch(this.selectedFile).subscribe({
      next: (response) => {
        this.uploading = false;
        if (response.data) {
          const batch = response.data as Batch;

          // ✅ Vérifier si le batch a été rejeté à cause d'erreurs
          if (batch.status === 'REJECTED' && batch.validationRemark) {
            const errorMsg = batch.validationRemark;
            console.log('🔍 Erreurs détectées:', errorMsg);

            // ✅ Extraire les erreurs ligne par ligne
            this.errorList = this.parseErrors(errorMsg);
            this.hasErrors = true;
            this.totalLines = batch.totalLines || 0;
            this.errorSummary = errorMsg;
            this.showErrorModal = true;

            this.toastService.error(`❌ ${this.errorList.length} erreur(s) détectée(s)`);
          } else {
            this.toastService.success('✅ Batch uploadé avec succès');
            this.router.navigate(['/batches']);
          }
        }
      },
      error: (err) => {
        this.uploading = false;
        console.error('❌ Erreur upload:', err);

        // ✅ Essayer d'extraire les erreurs de la réponse
        const errorBody = err.error;
        if (errorBody?.errorMessage) {
          const errorMsg = errorBody.errorMessage;
          this.errorList = this.parseErrors(errorMsg);
          this.hasErrors = true;
          this.errorSummary = errorMsg;
          this.showErrorModal = true;
          this.toastService.error(`❌ ${this.errorList.length} erreur(s) détectée(s)`);
        } else {
          this.toastService.error('❌ Erreur lors de l\'upload du fichier');
        }
      }
    });
  }

  // ✅ Parser les erreurs depuis le message
  private parseErrors(errorMessage: string): ErrorDetail[] {
    const errors: ErrorDetail[] = [];

    // Nettoyer le message
    let cleanMessage = errorMessage;
    if (errorMessage.includes('Erreurs IA :')) {
      cleanMessage = errorMessage.replace('Erreurs IA :', '').trim();
    }

    // Séparer par point-virgule ou retour à la ligne
    const parts = cleanMessage.split(/[;\n]/).filter(p => p.trim());

    for (const part of parts) {
      const trimmed = part.trim();
      if (!trimmed) continue;

      // Format: "Ligne X: erreur1, erreur2"
      const match = trimmed.match(/Ligne\s*(\d+)\s*:\s*(.+)/);
      if (match) {
        const lineNum = parseInt(match[1], 10);
        const errorText = match[2].trim();

        // Séparer les erreurs multiples (séparées par "," ou "et")
        const subErrors = errorText.split(/[,et]/).map(s => s.trim()).filter(s => s);
        for (const sub of subErrors) {
          errors.push({
            line: lineNum,
            field: this.detectField(sub),
            message: sub
          });
        }
      } else {
        // Erreur générale
        errors.push({
          line: 0,
          field: 'général',
          message: trimmed
        });
      }
    }

    return errors;
  }

  // ✅ Détecter le champ concerné par l'erreur
  private detectField(errorMessage: string): string {
    const lower = errorMessage.toLowerCase();
    if (lower.includes('msisdn') || lower.includes('numéro') || lower.includes('téléphone') || lower.includes('phone')) {
      return 'msisdn';
    }
    if (lower.includes('montant') || lower.includes('amount')) {
      return 'montant';
    }
    if (lower.includes('matricule')) {
      return 'matricule';
    }
    if (lower.includes('prenom') || lower.includes('prénom')) {
      return 'prenom';
    }
    if (lower.includes('nom')) {
      return 'nom';
    }
    if (lower.includes('remark') || lower.includes('motif')) {
      return 'remark';
    }
    return 'général';
  }

  downloadErrorReport(): void {
    let report = '=== RAPPORT D\'ERREURS BATCH ===\n';
    report += `Fichier: ${this.selectedFile?.name || 'Inconnu'}\n`;
    report += `Date: ${new Date().toLocaleString()}\n`;
    report += `Total des erreurs: ${this.errorList.length}\n\n`;
    report += 'Détail des erreurs:\n';
    report += '-'.repeat(50) + '\n';

    for (const err of this.errorList) {
      const lineInfo = err.line > 0 ? `Ligne ${err.line}` : 'Général';
      report += `${lineInfo} | Champ: ${err.field} | Erreur: ${err.message}\n`;
    }

    const blob = new Blob([report], { type: 'text/plain;charset=utf-8' });
    const link = document.createElement('a');
    link.href = URL.createObjectURL(blob);
    link.download = `rapport_erreurs_${new Date().toISOString().slice(0,10)}.txt`;
    link.click();
    URL.revokeObjectURL(link.href);
    this.toastService.success('📥 Rapport téléchargé');
  }

  closeErrorModal(): void {
    this.showErrorModal = false;
  }

  retryUpload(): void {
    this.clearErrors();
    // Garder le fichier sélectionné
    if (this.selectedFile) {
      this.uploadBatch();
    }
  }
}