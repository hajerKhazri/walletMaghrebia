import { HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';
import { Router } from '@angular/router';

export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const authService = inject(AuthService);
  const toastService = inject(ToastService);
  const router = inject(Router);

  return next(req).pipe(
    catchError((error: any) => {
      if (error.status === 401) {
        authService.clearAuth();
        router.navigate(['/auth/login']);
        toastService.error('Session expirée. Veuillez vous reconnecter.');
      } else if (error.status === 403) {
        toastService.error('Vous n\'avez pas les droits pour effectuer cette action.');
      } else if (error.status === 404) {
        toastService.error('Ressource non trouvée.');
      } else if (error.status === 500) {
        toastService.error('Erreur serveur. Veuillez réessayer plus tard.');
      } else if (error.error?.errorMessage) {
        toastService.error(error.error.errorMessage);
      } else if (error.error?.message) {
        toastService.error(error.error.message);
      }

      return throwError(() => error);
    })
  );
};