import { Component } from '@angular/core';
import { FormBuilder, Validators } from '@angular/forms';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { AuthService } from '../auth.service';

@Component({ selector: 'app-prijava', templateUrl: './prijava.component.html' })
export class PrijavaComponent {
  pending = false;
  error = '';
  registered = history.state?.registered === true;
  form = this.fb.group({ email: ['', [Validators.required, Validators.email]], lozinka: ['', Validators.required] });

  constructor(private fb: FormBuilder, private auth: AuthService, private router: Router) {}

  submit(): void {
    this.form.markAllAsTouched();
    if (this.form.invalid || this.pending) return;
    this.error = '';
    this.pending = true;
    const { email, lozinka } = this.form.value;
    this.auth.login(email, lozinka).pipe(finalize(() => this.pending = false)).subscribe({
      next: () => {
        this.form.reset();
        void this.router.navigate([this.auth.role === 'GRADJANIN' ? '/moji-zahtevi' : '/sluzbenik/zahtevi']);
      },
      error: error => this.error = this.auth.errorMessage(error)
    });
  }
}
