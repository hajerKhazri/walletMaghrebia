import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, ActivatedRoute, RouterModule } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-user-edit',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './user-edit.component.html',
  styleUrls: ['./user-edit.component.css']
})
export class UserEditComponent implements OnInit {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private toastService = inject(ToastService);
  private route = inject(ActivatedRoute);
  public router = inject(Router); // ✅ public pour le template

  userForm!: FormGroup;
  userId!: number;

  ngOnInit(): void {
    this.userId = +this.route.snapshot.paramMap.get('id')!;
    this.userForm = this.fb.group({
      firstName: ['', Validators.required],
      lastName: ['', Validators.required],
      email: ['', [Validators.required, Validators.email]],
      username: ['', Validators.required],
      role: ['', Validators.required]
    });

    this.userService.getUserById(this.userId).subscribe({
      next: (response) => {
        if (response.data) {
          this.userForm.patchValue(response.data);
        }
      },
      error: () => this.toastService.error('Erreur lors du chargement')
    });
  }

  onSubmit(): void {
    if (this.userForm.invalid) {
      this.toastService.warning('Veuillez remplir tous les champs');
      return;
    }
    this.userService.updateUser(this.userId, this.userForm.value).subscribe({
      next: () => {
        this.toastService.success('Utilisateur mis à jour');
        this.router.navigate(['/users']);
      },
      error: () => this.toastService.error('Erreur lors de la mise à jour')
    });
  }
}