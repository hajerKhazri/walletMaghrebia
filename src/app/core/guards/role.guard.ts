import { inject } from '@angular/core';
import { ActivatedRouteSnapshot, Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { ToastService } from '../services/toast.service';

export const roleGuard = (route: ActivatedRouteSnapshot) => {
  const authService = inject(AuthService);
  const router = inject(Router);
  const toastService = inject(ToastService);

  const expectedRoles = route.data['roles'] as string[];
  const user = authService.getCurrentUser();

  if (!user) {
    router.navigate(['/auth/login']);
    return false;
  }

  if (user.role === 'ADMIN') {
    return true;
  }

  if (expectedRoles && expectedRoles.includes(user.role)) {
    return true;
  }

  toastService.error('Vous n\'avez pas accès à cette page.');
  router.navigate(['/dashboard']);
  return false;
};