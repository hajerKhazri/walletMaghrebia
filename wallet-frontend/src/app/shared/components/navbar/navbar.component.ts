import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Router, RouterModule } from '@angular/router';
import { AuthService } from '../../../core/services/auth.service';
import { ToastService } from '../../../core/services/toast.service';

@Component({
  selector: 'app-navbar',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './navbar.component.html',
  styleUrls: ['./navbar.component.css']
})
export class NavbarComponent implements OnInit {
  private authService = inject(AuthService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  user: any;
  isAuthenticated = false;
  isMenuOpen = false;
  isDropdownOpen = false;

  ngOnInit(): void {
    this.authService.authState$.subscribe(state => {
      this.isAuthenticated = state.isAuthenticated;
      this.user = state.user;
    });
  }

  toggleMenu(): void {
    this.isMenuOpen = !this.isMenuOpen;
  }

  toggleDropdown(): void {
    this.isDropdownOpen = !this.isDropdownOpen;
  }

  logout(): void {
    this.authService.logout().subscribe({
      next: () => {
        this.authService.clearAuth();
        this.toastService.success('Déconnexion réussie');
        this.router.navigate(['/auth/login']);
      },
      error: () => {
        this.authService.clearAuth();
        this.router.navigate(['/auth/login']);
      }
    });
  }

  getRoleLabel(role: string): string {
    const labels: Record<string, string> = {
      'ADMIN': 'Administrateur',
      'INITIATEUR': 'Initiateur',
      'VALIDATEUR': 'Validateur',
      'FINANCE': 'Finance'
    };
    return labels[role] || role;
  }

  getInitials(): string {
    if (!this.user) return 'U';
    const first = this.user.firstName?.charAt(0) || this.user.username?.charAt(0) || 'U';
    const last = this.user.lastName?.charAt(0) || '';
    return (first + last).toUpperCase();
  }

  hasRole(role: string): boolean {
    return this.authService.hasRole(role);
  }
}