import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule, AbstractControl, ValidationErrors } from '@angular/forms';
import { Router, RouterModule } from '@angular/router';
import { UserService } from '../../../../core/services/user.service';
import { ToastService } from '../../../../core/services/toast.service';
import { Role } from '../../../../core/models/user.model';  // Assurez-vous que le chemin est correct

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule, RouterModule],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.css']
})
export class RegisterComponent {
  private fb = inject(FormBuilder);
  private userService = inject(UserService);
  private router = inject(Router);
  private toastService = inject(ToastService);

  registerForm: FormGroup;
  loading = false;
  showPassword = false;
  showConfirmPassword = false;

  constructor() {
    this.registerForm = this.fb.group({
      firstName: ['', [Validators.required, Validators.minLength(2)]],
      lastName: ['', [Validators.required, Validators.minLength(2)]],
      username: ['', [Validators.required, Validators.minLength(3)]],
      email: ['', [Validators.required, Validators.email]],
      password: ['', [
        Validators.required,
        Validators.minLength(8)
      ]],
      confirmPassword: ['', [Validators.required]],
      acceptTerms: [false, [Validators.requiredTrue]]
    }, {
      validators: this.passwordMatchValidator
    });
  }

  // Validateur personnalisé : vérifie que les mots de passe correspondent
  passwordMatchValidator(group: AbstractControl): ValidationErrors | null {
    const password = group.get('password')?.value;
    const confirm = group.get('confirmPassword')?.value;
    return password === confirm ? null : { mismatch: true };
  }

  onSubmit(): void {
    // 1. Vérification de la validité du formulaire
    if (this.registerForm.invalid) {
      this.toastService.warning('Veuillez remplir tous les champs correctement');
      // On affiche aussi les erreurs dans la console pour faciliter le débogage
      console.warn('❌ Formulaire invalide :', this.registerForm.errors);
      Object.keys(this.registerForm.controls).forEach(key => {
        const control = this.registerForm.get(key);
        if (control?.invalid) {
          console.warn(`  - ${key} :`, control.errors);
        }
      });
      return;
    }

    this.loading = true;

    // 2. Récupération des données du formulaire
    const formData = this.registerForm.value;

    // 3. Construction de l'objet utilisateur à envoyer
    //    On s'assure que le champ 'password' est bien présent et non vide
    const userData = {
      username: formData.username.trim(),
      email: formData.email.trim().toLowerCase(),
      password: formData.password,   // <- ICI LE MOT DE PASSE EST BIEN INCLUS
      firstName: formData.firstName.trim(),
      lastName: formData.lastName.trim(),
      role: Role.INITIATEUR  // Utilisation de l'enum Role (vérifiez l'import)
    };

    // 4. Log détaillé pour vérifier les données envoyées
    console.log('📤 Données envoyées au backend :', {
      ...userData,
      password: '***'   // Masqué pour la sécurité dans la console
    });

    // 5. Appel au service
    this.userService.createUser(userData).subscribe({
      next: (response) => {
        console.log('✅ Inscription réussie :', response);
        this.toastService.success('Compte créé avec succès ! Vous pouvez maintenant vous connecter.');
        this.router.navigate(['/auth/login']);
        this.loading = false;
      },
      error: (error) => {
        this.loading = false;
        console.error('❌ Erreur lors de l\'inscription :', error);

        // Récupération du message d'erreur renvoyé par le backend
        let errorMsg = 'Erreur lors de la création du compte.';
        if (error.error?.errorMessage) {
          errorMsg = error.error.errorMessage;
        } else if (error.error?.message) {
          errorMsg = error.error.message;
        } else if (error.message) {
          errorMsg = error.message;
        }

        this.toastService.error(errorMsg);
      }
    });
  }

  // Gestion de l'affichage du mot de passe (toggle)
  togglePassword(): void {
    this.showPassword = !this.showPassword;
  }

  toggleConfirmPassword(): void {
    this.showConfirmPassword = !this.showConfirmPassword;
  }

  // Fonctions pour la force du mot de passe (optionnelles, vous pouvez les garder)
  getPasswordStrength(password: string): number {
    if (!password) return 0;
    let strength = 0;
    if (password.length >= 8) strength++;
    if (/[a-z]/.test(password)) strength++;
    if (/[A-Z]/.test(password)) strength++;
    if (/\d/.test(password)) strength++;
    if (/[@$!%*?&]/.test(password)) strength++;
    return strength;
  }

  getPasswordStrengthLabel(strength: number): string {
    const labels = ['', 'Très faible', 'Faible', 'Moyen', 'Fort', 'Très fort'];
    return labels[strength] || '';
  }

  getPasswordStrengthColor(strength: number): string {
    const colors = ['', '#DC2626', '#D97706', '#F59E0B', '#059669', '#10B981'];
    return colors[strength] || '';
  }
}