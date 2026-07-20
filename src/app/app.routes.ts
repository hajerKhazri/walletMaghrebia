import { Routes } from '@angular/router';
import { authGuard } from './core/guards/auth.guard';
import { roleGuard } from './core/guards/role.guard';

export const routes: Routes = [
  { path: '', redirectTo: '/dashboard', pathMatch: 'full' },
  { path: 'auth/login', loadComponent: () => import('./modules/auth/components/login/login.component').then(m => m.LoginComponent) },
  { path: 'auth/register', loadComponent: () => import('./modules/auth/components/register/register.component').then(m => m.RegisterComponent) },
  { path: 'auth/forgot-password', loadComponent: () => import('./modules/auth/components/forgot-password/forgot-password.component').then(m => m.ForgotPasswordComponent) },
  { path: 'auth/reset-password', loadComponent: () => import('./modules/auth/components/reset-password/reset-password.component').then(m => m.ResetPasswordComponent) },
  { path: 'dashboard', loadComponent: () => import('./modules/dashboard/components/dashboard/dashboard.component').then(m => m.DashboardComponent), canActivate: [authGuard] },
  { path: 'transactions', loadComponent: () => import('./modules/transactions/components/transaction-list/transaction-list.component').then(m => m.TransactionListComponent), canActivate: [authGuard] },
  { path: 'transactions/create', loadComponent: () => import('./modules/transactions/components/transaction-create/transaction-create.component').then(m => m.TransactionCreateComponent), canActivate: [authGuard, roleGuard], data: { roles: ['INITIATEUR', 'ADMIN'] } },
  { path: 'transactions/:id', loadComponent: () => import('./modules/transactions/components/transaction-detail/transaction-detail.component').then(m => m.TransactionDetailComponent), canActivate: [authGuard] },
  { path: 'batches', loadComponent: () => import('./modules/batches/components/batch-list/batch-list.component').then(m => m.BatchListComponent), canActivate: [authGuard] },
  { path: 'batches/upload', loadComponent: () => import('./modules/batches/components/batch-upload/batch-upload.component').then(m => m.BatchUploadComponent), canActivate: [authGuard, roleGuard], data: { roles: ['INITIATEUR', 'ADMIN'] } },
  { path: 'users', loadComponent: () => import('./modules/users/components/user-list/user-list.component').then(m => m.UserListComponent), canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  // ✅ Route pour l'édition d'utilisateur
  { path: 'users/edit/:id', loadComponent: () => import('./modules/users/components/user-edit/user-edit.component').then(m => m.UserEditComponent), canActivate: [authGuard, roleGuard], data: { roles: ['ADMIN'] } },
  { path: '**', redirectTo: '/dashboard' }
];