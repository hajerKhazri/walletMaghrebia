import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';

@Component({
  selector: 'app-forgot-password',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './forgot-password.component.html',
  styleUrls: ['./forgot-password.component.css']
})
export class ForgotPasswordComponent {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private toastService = inject(ToastService);

  forgotForm: FormGroup;
  loading = false;
  emailSent = false;

  constructor() {
    this.forgotForm = this.fb.group({
      email: ['', [Validators.required, Validators.email]]
    });
  }

  onSubmit(): void {
    if (this.forgotForm.invalid) {
      this.toastService.warning('Veuillez saisir un email valide');
      return;
    }

    this.loading = true;
    const email = this.forgotForm.get('email')?.value;

    this.userService.sendPasswordResetEmail(email).subscribe({
      next: () => {
        this.emailSent = true;
        this.loading = false;
        this.toastService.success('Un email de réinitialisation a été envoyé');
      },
      error: () => {
        this.loading = false;
        this.emailSent = true;
        this.toastService.info('Si cet email existe, vous recevrez un lien de réinitialisation');
      }
    });
  }

  resendEmail(): void {
    this.emailSent = false;
    this.onSubmit();
  }
}