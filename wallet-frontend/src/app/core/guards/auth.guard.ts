import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';

export const authGuard = () => {
  const authService = inject(AuthService);
  const router = inject(Router);

  const isAuthenticated = authService.isAuthenticated();
  const token = authService.getAccessToken();
  const user = authService.getCurrentUser();

  console.log('🔒 AuthGuard - Vérification:');
  console.log('  - isAuthenticated:', isAuthenticated);
  console.log('  - token:', token ? '✅ présent' : '❌ absent');
  console.log('  - user:', user ? '✅ présent' : '❌ absent');

  if (isAuthenticated && token && user) {
    console.log('✅ AuthGuard - Accès autorisé');
    return true;
  }

  console.log('❌ AuthGuard - Accès refusé, redirection vers login');
  authService.clearAuth();
  router.navigate(['/auth/login']);
  return false;
};