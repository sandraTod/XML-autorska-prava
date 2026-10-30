import { Component } from '@angular/core';
import { AbstractControl, FormBuilder, ValidationErrors, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../auth.service';

@Component({ selector: 'app-registracija', templateUrl: './registracija.component.html' })
export class RegistracijaComponent {
  pending = false;
  error = '';
  form = this.fb.group({
    ime: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
    prezime: ['', [Validators.required, Validators.pattern(/.*\S.*/)]],
    email: ['', [Validators.required, Validators.email]],
    lozinka: ['', [Validators.required, this.passwordValidator]]
  });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.pending) return;
    this.error = '';
    this.pending = true;
    const { ime, prezime, email, lozinka } = this.form.value;
    this.auth.register(ime, prezime, email, lozinka).pipe(finalize(() => this.pending = false)).subscribe({
      next: () => {
        this.form.reset();
        void this.router.navigate(['/auth/prijava'], { state: { registered: true } });
      },
      error: error => this.error = this.auth.errorMessage(error)
    });
  }

  private passwordValidator(control: AbstractControl): ValidationErrors | null {
    const password: string = control.value || '';
    return Array.from(password).length >= 8 && new TextEncoder().encode(password).length <= 72 ? null : { password: true };
  }
}
