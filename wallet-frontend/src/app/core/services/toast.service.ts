import { Injectable, inject } from '@angular/core';
import { ToastrService } from 'ngx-toastr';

@Injectable({
  providedIn: 'root'
})
export class ToastService {
  private toastr = inject(ToastrService);

  success(message: string, title?: string): void {
    this.toastr.success(message, title || 'Succès');
  }

  error(message: string, title?: string): void {
    this.toastr.error(message, title || 'Erreur');
  }

  info(message: string, title?: string): void {
    this.toastr.info(message, title || 'Information');
  }

  warning(message: string, title?: string): void {
    this.toastr.warning(message, title || 'Attention');
  }
}