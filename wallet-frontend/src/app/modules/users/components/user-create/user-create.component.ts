import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Role } from '../../../../core/models/user.model';

@Component({
  selector: 'app-user-create',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './user-create.component.html',
  styleUrls: ['./user-create.component.css']
})
export class UserCreateComponent {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private router = inject(Router);

  userForm: FormGroup;
  loading = false;
  roles = Object.values(Role);

  constructor() {
    this.userForm = this.fb.group({
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [Validators.required, Validators.minLength(6)]],
      firstName: ['', [Validators.required]],
      lastName: ['', [Validators.required]],
      role: ['INITIATEUR', [Validators.required]]
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid) {
      this.toastService.warning('Veuillez remplir tous les champs obligatoires');
      return;
    }

    this.loading = true;
    this.userService.createUser(this.userForm.value).subscribe({
      next: (response) => {
        if (response.data) {
          this.toastService.success(`Utilisateur ${response.data.username} créé avec succès`);
          this.router.navigate(['/users']);
        }
        this.loading = false;
      },
      error: () => {
        this.loading = false;
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
}