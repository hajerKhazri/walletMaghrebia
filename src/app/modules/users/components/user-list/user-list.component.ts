import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { RouterModule, Router } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { User } from '../../../../core/models/user.model';

@Component({
  selector: 'app-user-list',
  standalone: true,
  imports: [CommonModule, RouterModule],
  templateUrl: './user-list.component.html',
  styleUrls: ['./user-list.component.css']
})
export class UserListComponent implements OnInit {
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  users: User[] = [];
  loading = false;

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers(): void {
    this.loading = true;
    this.userService.getAllUsers().subscribe({
      next: (response) => {
        if (response.data && response.data.content) {
          this.users = response.data.content;
        } else {
          this.users = [];
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
        this.toastService.error('Erreur lors du chargement des utilisateurs');
      }
    });
  }

  editUser(user: User): void {
    this.router.navigate(['/users/edit', user.id]);
  }

 toggleUserStatus(user: User): void {
  const action = user.active ? 'désactiver' : 'activer';
  if (!confirm(`Voulez-vous vraiment ${action} l'utilisateur "${user.username}" ?`)) return;

  if (user.active) {
    this.userService.deactivateUser(user.id).subscribe({
      next: () => {
        this.toastService.success('Utilisateur désactivé avec succès');
        user.active = false;
      },
      error: () => this.toastService.error('Erreur lors de la désactivation')
    });
  } else {
    this.userService.activateUser(user.id).subscribe({
      next: () => {
        this.toastService.success('Utilisateur activé avec succès');
        user.active = true;
      },
      error: () => this.toastService.error('Erreur lors de l\'activation')
    });
  }
}

  changeRole(event: Event, user: User): void {
    const select = event.target as HTMLSelectElement;
    const newRole = select.value;
    if (newRole === user.role) return;
    if (!confirm(`Changer le rôle de "${user.username}" en ${newRole} ?`)) {
      select.value = user.role;
      return;
    }
    this.userService.changeUserRole(user.id, newRole).subscribe({
      next: (response) => {
        if (response.data) {
          user.role = response.data.role;
          this.toastService.success('Rôle modifié');
        }
      },
      error: () => {
        this.toastService.error('Erreur lors du changement de rôle');
        select.value = user.role;
      }
    });
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

  getRoleClass(role: string): string {
    const map: Record<string,string> = {
      'ADMIN': 'badge-danger',
      'INITIATEUR': 'badge-primary',
      'VALIDATEUR': 'badge-success',
      'FINANCE': 'badge-warning'
    };
    return map[role] || 'badge-secondary';
  }

  getRoleOptions(): string[] {
    return ['ADMIN', 'INITIATEUR', 'VALIDATEUR', 'FINANCE'];
  }

  formatDate(date: string): string {
    return date ? new Date(date).toLocaleString('fr-FR') : '-';
  }
}